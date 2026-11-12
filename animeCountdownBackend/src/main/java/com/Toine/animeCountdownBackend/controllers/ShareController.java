package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.services.share.ShareCard;
import com.Toine.animeCountdownBackend.services.share.ShareCardRenderer;
import com.Toine.animeCountdownBackend.services.share.ShareCardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;


/**
 * Link previews for show pages.
 *
 * The site is a client-rendered SPA, so link-preview bots (Discord, X, Slack, iMessage, …) never see
 * per-show tags. Vercel rewrites /media/:id requests from those bots to {@code /api/share/media/:id}
 * (see animeCountdownApp/vercel.json), which returns a small HTML page with Open Graph / Twitter
 * tags. Its og:image is {@code card.jpg}, drawn on the fly with the current countdown.
 */
@RestController
public class ShareController {

    /** How long previews may be cached. Short, so the countdown in them stays roughly right. */
    static final Duration TTL = Duration.ofMinutes(5);
    private static final CacheControl CACHE = CacheControl.maxAge(TTL).cachePublic().sMaxAge(TTL);
    private static final int DESCRIPTION_MAX = 200;

    private final ShareCardService shareCardService;
    private final ShareCardRenderer renderer;
    private final String siteUrl;
    private final String apiUrl;

    public ShareController(ShareCardService shareCardService, ShareCardRenderer renderer,
                           @Value("${app.share.site-url:https://animelivecountdown.com}") String siteUrl,
                           @Value("${app.share.api-url:https://api.animelivecountdown.com}") String apiUrl) {
        this.shareCardService = shareCardService;
        this.renderer = renderer;
        this.siteUrl = stripSlash(siteUrl);
        this.apiUrl = stripSlash(apiUrl);
    }

    @GetMapping(value = "/api/share/media/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@PathVariable long id) {
        Optional<ShareCard> card = shareCardService.find(id);
        if (card.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CACHE)
                    .contentType(MediaType.TEXT_HTML).body(notFoundPage());
        }
        return ResponseEntity.ok().cacheControl(CACHE).contentType(MediaType.TEXT_HTML)
                .body(html(card.get()));
    }

    @GetMapping("/api/share/media/{id}/card.jpg")
    public ResponseEntity<byte[]> image(@PathVariable long id) {
        Optional<ShareCard> card = shareCardService.find(id);
        if (card.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<byte[]> jpeg = renderer.render(card.get());
        if (jpeg.isPresent()) {
            return ResponseEntity.ok().cacheControl(CACHE).contentType(MediaType.IMAGE_JPEG).body(jpeg.get());
        }
        // Couldn't draw the card; the cover art is still a decent preview
        String fallback = card.get().coverUrl() != null ? card.get().coverUrl() : siteUrl + "/images/preview-image.jpg";
        return ResponseEntity.status(HttpStatus.FOUND).cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)))
                .location(URI.create(fallback)).build();
    }

    String html(ShareCard card) {
        String pageUrl = siteUrl + "/media/" + card.mediaId();
        // A new image URL every 5 minutes, so platforms that cache by URL pick up the new countdown
        long version = shareCardService.now().getEpochSecond() / TTL.toSeconds();
        String imageUrl = apiUrl + "/api/share/media/" + card.mediaId() + "/card.jpg?v=" + version;
        String title = card.title() + " — " + card.countdown();
        String description = description(card);

        String t = htmlEscape(title);
        String d = htmlEscape(description);
        String u = htmlEscape(pageUrl);
        String i = htmlEscape(imageUrl);
        String alt = htmlEscape(card.title() + " cover art with the next-episode countdown");
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>%1$s | Anime Live Countdown</title>
                <meta name="description" content="%2$s">
                <link rel="canonical" href="%3$s">
                <meta property="og:site_name" content="Anime Live Countdown">
                <meta property="og:type" content="website">
                <meta property="og:title" content="%1$s">
                <meta property="og:description" content="%2$s">
                <meta property="og:url" content="%3$s">
                <meta property="og:image" content="%4$s">
                <meta property="og:image:secure_url" content="%4$s">
                <meta property="og:image:type" content="image/jpeg">
                <meta property="og:image:width" content="%6$d">
                <meta property="og:image:height" content="%7$d">
                <meta property="og:image:alt" content="%5$s">
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:title" content="%1$s">
                <meta name="twitter:description" content="%2$s">
                <meta name="twitter:image" content="%4$s">
                <meta name="twitter:image:alt" content="%5$s">
                <meta name="theme-color" content="#00a6fb">
                </head>
                <body>
                <h1>%1$s</h1>
                <p>%2$s</p>
                <p><a href="%3$s">Open on Anime Live Countdown</a></p>
                </body>
                </html>
                """.formatted(t, d, u, i, alt, ShareCardRenderer.WIDTH, ShareCardRenderer.HEIGHT);
    }

    static String description(ShareCard card) {
        StringBuilder sb = new StringBuilder(card.countdown());
        if (card.airingLine() != null) {
            sb.append(" (").append(card.airingLine()).append(")");
        }
        sb.append('.');
        if (card.synopsis() != null && !card.synopsis().isEmpty()) {
            sb.append(' ').append(card.synopsis());
        }
        if (sb.length() > DESCRIPTION_MAX) {
            int cut = sb.lastIndexOf(" ", DESCRIPTION_MAX - 1);
            sb.setLength(cut > DESCRIPTION_MAX / 2 ? cut : DESCRIPTION_MAX - 1);
            sb.append('…');
        }
        return sb.toString();
    }

    private String notFoundPage() {
        String u = htmlEscape(siteUrl + "/");
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="utf-8">
                <title>Anime Live Countdown</title>
                <meta property="og:title" content="Anime Live Countdown">
                <meta property="og:description" content="Live countdowns for upcoming anime episodes.">
                <meta property="og:url" content="%1$s">
                <meta property="og:image" content="%1$simages/preview-image.jpg">
                <meta name="twitter:card" content="summary_large_image">
                </head><body><a href="%1$s">Anime Live Countdown</a></body></html>
                """.formatted(u);
    }

    /** Escapes only the markup-significant characters, leaving accents and dashes readable. */
    private static String htmlEscape(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
