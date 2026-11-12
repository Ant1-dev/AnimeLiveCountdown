package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.services.share.ShareCard;
import com.Toine.animeCountdownBackend.services.share.ShareCardRenderer;
import com.Toine.animeCountdownBackend.services.share.ShareCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShareControllerTest {

    private static final ShareCard CARD = new ShareCard(171018, "Frieren <Beyond> \"Journey's\" End",
            "Ep 5 airs in 2d 4h", "Fri, Nov 13 at 15:00 UTC", "An elf mage outlives her party.",
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/x.jpg", null);

    private ShareCardService service;
    private ShareCardRenderer renderer;
    private ShareController controller;

    @BeforeEach
    void setUp() {
        service = mock(ShareCardService.class);
        renderer = mock(ShareCardRenderer.class);
        when(service.now()).thenReturn(Instant.ofEpochSecond(3_000));
        controller = new ShareController(service, renderer, "https://animelivecountdown.com/", "https://api.example.com");
    }

    @Test
    void pageHasPerShowOpenGraphAndTwitterTags() {
        when(service.find(171018)).thenReturn(Optional.of(CARD));

        ResponseEntity<String> response = controller.page(171018);
        String html = response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getCacheControl().contains("max-age=300"));
        assertTrue(html.contains("<meta property=\"og:url\" content=\"https://animelivecountdown.com/media/171018\">"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://api.example.com/api/share/media/171018/card.jpg?v=10\">"));
        assertTrue(html.contains("<meta name=\"twitter:card\" content=\"summary_large_image\">"));
        assertTrue(html.contains("Ep 5 airs in 2d 4h (Fri, Nov 13 at 15:00 UTC). An elf mage outlives her party."));
    }

    @Test
    void titlesAreHtmlEscaped() {
        when(service.find(171018)).thenReturn(Optional.of(CARD));

        String html = controller.page(171018).getBody();

        assertFalse(html.contains("<Beyond>"));
        assertTrue(html.contains("Frieren &lt;Beyond&gt; &quot;Journey&#39;s&quot; End — Ep 5 airs in 2d 4h"));
    }

    @Test
    void unknownShowIs404WithGenericTags() {
        when(service.find(1)).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.page(1);

        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().contains("og:title"));
    }

    @Test
    void imageFallsBackToCoverWhenRenderingFails() {
        when(service.find(171018)).thenReturn(Optional.of(CARD));
        when(renderer.render(CARD)).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = controller.image(171018);

        assertEquals(302, response.getStatusCode().value());
        assertEquals(CARD.coverUrl(), response.getHeaders().getLocation().toString());
    }

    @Test
    void longDescriptionsAreCut() {
        ShareCard wordy = new ShareCard(1, "T", "Coming soon", null, "word ".repeat(100), null, null);

        String description = ShareController.description(wordy);

        assertTrue(description.length() <= 201);
        assertTrue(description.endsWith("…"));
    }
}
