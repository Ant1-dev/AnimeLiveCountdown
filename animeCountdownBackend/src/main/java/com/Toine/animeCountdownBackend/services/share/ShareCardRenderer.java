package com.Toine.animeCountdownBackend.services.share;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Draws the 1200×630 Open Graph image for a show with Java2D: blurred banner background, cover art,
 * title and countdown. Output is JPEG (small, and accepted by every link-preview crawler).
 *
 * Only images on AniList's CDN are fetched. Fetched image bytes and rendered cards are kept in small
 * in-memory LRU caches; the rendered-card key includes the countdown text, so a card is redrawn
 * whenever its countdown changes.
 */
@Component
public class ShareCardRenderer {

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 630;

    private static final Logger log = LoggerFactory.getLogger(ShareCardRenderer.class);
    private static final Color BACKGROUND = new Color(0x15, 0x17, 0x24);
    private static final Color ACCENT = new Color(0x00, 0xA6, 0xFB);
    private static final Color MUTED = new Color(0xB8, 0xC0, 0xD4);
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final Map<String, byte[]> sourceCache = lru(128);
    private final Map<String, byte[]> cardCache = lru(64);
    // Rendering is CPU-bound; don't let a burst of crawler hits pile up renders
    private final Semaphore renderSlots = new Semaphore(2);

    /** JPEG bytes, or empty if the card could not be drawn (the caller falls back to the cover art). */
    public Optional<byte[]> render(ShareCard card) {
        String key = card.mediaId() + "|" + card.title() + "|" + card.countdown() + "|" + card.coverUrl() + "|" + card.bannerUrl();
        byte[] cached = cardCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            if (!renderSlots.tryAcquire(10, TimeUnit.SECONDS)) {
                return Optional.empty();
            }
            try {
                byte[] jpeg = toJpeg(draw(card, loadImage(card.coverUrl()), loadImage(card.bannerUrl())));
                cardCache.put(key, jpeg);
                return Optional.of(jpeg);
            } finally {
                renderSlots.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception | Error e) {
            // e.g. missing fonts/native libraries on a slim JRE image
            log.warn("Could not render share card for media {}: {}", card.mediaId(), e.toString());
            return Optional.empty();
        }
    }

    BufferedImage draw(ShareCard card, BufferedImage cover, BufferedImage banner) {
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.setColor(BACKGROUND);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            BufferedImage backdrop = banner != null ? banner : cover;
            if (backdrop != null) {
                drawCover(g, blur(backdrop), 0, 0, WIDTH, HEIGHT);
                g.setPaint(new GradientPaint(0, 0, new Color(0x15, 0x17, 0x24, 200),
                        WIDTH, 0, new Color(0x15, 0x17, 0x24, 235)));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }

            int coverX = 64, coverY = 65, coverW = 333, coverH = 500;
            if (cover != null) {
                g.setColor(new Color(0, 0, 0, 90));
                g.fill(new RoundRectangle2D.Float(coverX + 6, coverY + 10, coverW, coverH, 24, 24));
                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Float(coverX, coverY, coverW, coverH, 24, 24));
                drawCover(g, cover, coverX, coverY, coverW, coverH);
                g.setClip(oldClip);
            }

            int textX = cover != null ? coverX + coverW + 56 : 80;
            int textW = WIDTH - textX - 64;

            g.setColor(ACCENT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.drawString("ANIME LIVE COUNTDOWN", textX, 118);

            Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 58);
            List<String> titleLines = wrap(card.title(), g.getFontMetrics(titleFont), textW, 3);
            if (titleLines.size() == 3) {
                titleFont = titleFont.deriveFont(48f);
                titleLines = wrap(card.title(), g.getFontMetrics(titleFont), textW, 3);
            }
            g.setFont(titleFont);
            g.setColor(Color.WHITE);
            FontMetrics tm = g.getFontMetrics();
            int y = 140 + tm.getAscent();
            for (String line : titleLines) {
                g.drawString(line, textX, y);
                y += tm.getHeight();
            }

            // Countdown pill
            Font pillFont = new Font(Font.SANS_SERIF, Font.BOLD, 44);
            FontMetrics pm = g.getFontMetrics(pillFont);
            String countdown = ellipsize(card.countdown(), pm, textW - 56);
            int pillW = pm.stringWidth(countdown) + 56;
            int pillH = 84;
            int pillY = Math.max(y + 10, 380);
            g.setColor(ACCENT);
            g.fill(new RoundRectangle2D.Float(textX, pillY, pillW, pillH, pillH, pillH));
            g.setColor(Color.WHITE);
            g.setFont(pillFont);
            g.drawString(countdown, textX + 28, pillY + (pillH - pm.getHeight()) / 2 + pm.getAscent());

            if (card.airingLine() != null) {
                g.setColor(MUTED);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 26));
                g.drawString(card.airingLine(), textX + 4, pillY + pillH + 42);
            }

            g.setColor(MUTED);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 26));
            g.drawString("animelivecountdown.com", textX, HEIGHT - 64);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /** Draws img scaled to fill the box, cropping the overflow (CSS object-fit: cover). */
    private static void drawCover(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) Math.ceil(img.getWidth() * scale);
        int dh = (int) Math.ceil(img.getHeight() * scale);
        g.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
    }

    /** Cheap blur: shrink a lot, the bilinear upscale in drawCover smooths it out. */
    private static BufferedImage blur(BufferedImage img) {
        int w = Math.max(1, img.getWidth() / 16);
        int h = Math.max(1, img.getHeight() / 16);
        BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return small;
    }

    static List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String[] words = text.trim().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String candidate = line.isEmpty() ? words[i] : line + " " + words[i];
            if (fm.stringWidth(candidate) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            lines.add(line.toString());
            line.setLength(0);
            line.append(words[i]);
            if (lines.size() == maxLines - 1) {
                // Last line gets everything that's left, cut with an ellipsis
                StringBuilder rest = new StringBuilder(line);
                for (int j = i + 1; j < words.length; j++) {
                    rest.append(' ').append(words[j]);
                }
                lines.add(ellipsize(rest.toString(), fm, maxWidth));
                return lines;
            }
        }
        if (!line.isEmpty()) {
            lines.add(ellipsize(line.toString(), fm, maxWidth));
        }
        return lines;
    }

    static String ellipsize(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && fm.stringWidth(cut + "…") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "…";
    }

    private BufferedImage loadImage(String url) {
        if (!isAllowedImageUrl(url)) {
            return null;
        }
        try {
            byte[] bytes = sourceCache.get(url);
            if (bytes == null) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .header("User-Agent", "AnimeLiveCountdown-ShareCard/1.0")
                        .GET().build();
                HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    if (response.statusCode() != 200) {
                        return null;
                    }
                    bytes = body.readNBytes(MAX_IMAGE_BYTES + 1);
                }
                if (bytes.length > MAX_IMAGE_BYTES) {
                    return null;
                }
                sourceCache.put(url, bytes);
            }
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.debug("Could not load share card image {}: {}", url, e.toString());
            return null;
        }
    }

    /** Only https images on AniList's CDN; the URLs come from synced data, but don't fetch anything else. */
    static boolean isAllowedImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null
                    && (host.equals("anilist.co") || host.endsWith(".anilist.co") || host.equals("img.anili.st"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] toJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream(160_000);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.88f);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static <V> Map<String, V> lru(int maxEntries) {
        return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > maxEntries;
            }
        });
    }
}
