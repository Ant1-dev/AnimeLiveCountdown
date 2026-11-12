package com.Toine.animeCountdownBackend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Slugs must match the frontend's slug.ts exactly, or sitemap/share links and canonical URLs
 * would disagree. Both sides are checked against the same vectors
 * (the frontend via `npm run test:slugs`).
 */
class SlugsTest {

    @Test
    void matchesSharedVectors() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/slug-vectors.json")) {
            List<List<String>> vectors = new ObjectMapper().readValue(in, List.class);
            for (List<String> v : vectors) {
                assertEquals(v.get(1), Slugs.slugify(v.get(0)), "slug of " + v.get(0));
            }
        }
    }

    @Test
    void buildsCanonicalPaths() {
        assertEquals("/anime/21/one-piece", Slugs.animePath(21, "ONE PIECE"));
        assertEquals("/anime/5", Slugs.animePath(5, "進撃の巨人"));
        assertEquals("/anime/7", Slugs.animePath(7, null));
    }
}
