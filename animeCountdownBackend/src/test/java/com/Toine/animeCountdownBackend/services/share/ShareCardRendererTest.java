package com.Toine.animeCountdownBackend.services.share;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ShareCardRendererTest {

    private final ShareCardRenderer renderer = new ShareCardRenderer();

    @Test
    void drawsAFullSizeCardWithoutAnyArt() {
        ShareCard card = new ShareCard(1, "A Very Long Anime Title That Needs To Wrap Onto Several Lines Of The Card",
                "Ep 12 airs in 6d 23h", "Tue, Nov 17 at 15:00 UTC", "", null, null);

        BufferedImage image = renderer.draw(card, null, null);

        assertEquals(ShareCardRenderer.WIDTH, image.getWidth());
        assertEquals(ShareCardRenderer.HEIGHT, image.getHeight());
    }

    @Test
    void rendersJpegBytes() {
        ShareCard card = new ShareCard(2, "Short", "Coming soon", null, "", null, null);

        byte[] jpeg = renderer.render(card).orElseThrow();

        // JPEG magic number
        assertEquals((byte) 0xFF, jpeg[0]);
        assertEquals((byte) 0xD8, jpeg[1]);
    }

    @Test
    void onlyFetchesHttpsImagesFromAniList() {
        assertTrue(ShareCardRenderer.isAllowedImageUrl("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/x.jpg"));
        assertTrue(ShareCardRenderer.isAllowedImageUrl("https://img.anili.st/media/1"));
        assertFalse(ShareCardRenderer.isAllowedImageUrl("http://s4.anilist.co/x.jpg"));
        assertFalse(ShareCardRenderer.isAllowedImageUrl("https://evil.example/anilist.co.jpg"));
        assertFalse(ShareCardRenderer.isAllowedImageUrl("https://anilist.co.evil.example/x.jpg"));
        assertFalse(ShareCardRenderer.isAllowedImageUrl("http://169.254.169.254/latest/meta-data"));
        assertFalse(ShareCardRenderer.isAllowedImageUrl(null));
    }
}
