package com.Toine.animeCountdownBackend.utils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * URL slugs for show pages: /anime/21/one-piece.
 *
 * Must stay identical to animeCountdownApp/src/app/seo/slug.ts (the frontend's canonical URLs).
 * Both are tested against src/test/resources/slug-vectors.json.
 */
public final class Slugs {
    private static final int MAX_LENGTH = 80;

    private Slugs() {
    }

    public static String slugify(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }
        String s = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("[\\u0300-\\u036f]", "") // strip accents: Pokémon -> Pokemon
                .toLowerCase(Locale.ROOT)
                .replaceAll("['\\u2019]", "") // drop apostrophes: Frieren's -> frierens
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH);
        }
        return s.replaceAll("-+$", "");
    }

    /** Canonical path of a show page; /anime/:id when the title has no Latin characters. */
    public static String animePath(long id, String title) {
        String slug = slugify(title);
        return slug.isEmpty() ? "/anime/" + id : "/anime/" + id + "/" + slug;
    }
}
