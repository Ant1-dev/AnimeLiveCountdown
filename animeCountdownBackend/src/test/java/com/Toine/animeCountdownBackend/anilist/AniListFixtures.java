package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.ListCollection;
import com.Toine.animeCountdownBackend.anilist.AniListModels.ListEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Loads the recorded-shape AniList MediaListCollection response used by the import tests. */
final class AniListFixtures {
    private AniListFixtures() {
    }

    static List<ListEntry> listEntries() {
        try (InputStream in = AniListFixtures.class.getResourceAsStream("/anilist/media-list-collection.json")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            ListCollection collection = mapper.treeToValue(root.at("/data/MediaListCollection"), ListCollection.class);
            return AniListApiClient.flatten(collection);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
