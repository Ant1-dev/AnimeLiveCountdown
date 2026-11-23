package com.Toine.animeCountdownBackend.services.watch;

import com.Toine.animeCountdownBackend.models.discovery.StreamingLink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StreamingServicesTest {

    @Test
    void aniListSiteNamesMapToServiceKeys() {
        assertEquals(Optional.of("amazon"), StreamingServices.keyFor("Amazon Prime Video"));
        assertEquals(Optional.of("amazon"), StreamingServices.keyFor("Prime Video"));
        assertEquals(Optional.of("disney"), StreamingServices.keyFor("Disney Plus"));
        assertEquals(Optional.of("disney"), StreamingServices.keyFor("Disney+"));
        assertEquals(Optional.of("bilibili"), StreamingServices.keyFor("Bilibili TV"));
        assertEquals(Optional.of("tubi"), StreamingServices.keyFor("Tubi TV"));
        assertEquals(Optional.of("iqiyi"), StreamingServices.keyFor("iQ"));
        assertEquals(Optional.of("hidive"), StreamingServices.keyFor("HIDIVE"));
        assertEquals(Optional.of("crunchyroll"), StreamingServices.keyFor("Funimation"));
    }

    @Test
    void animeSchedulePlatformKeysMapToTheSameServices() {
        for (String key : List.of("crunchyroll", "netflix", "hidive", "hulu", "disney", "amazon", "apple",
                "youtube", "bilibili", "oceanveil")) {
            assertEquals(Optional.of(key), StreamingServices.keyFor(key), key);
        }
    }

    @Test
    void unknownSitesHaveNoKey() {
        assertTrue(StreamingServices.keyFor("Some Local Site").isEmpty());
        assertTrue(StreamingServices.keyFor(null).isEmpty());
        assertTrue(StreamingServices.keyFor("").isEmpty());
    }

    @Test
    void catalogKeysAreUniqueAndKnown() {
        var keys = StreamingServices.all().stream().map(StreamingServices.Service::key).toList();
        assertEquals(keys.size(), Set.copyOf(keys).size());
        assertEquals("crunchyroll", keys.get(0));
        keys.forEach(k -> assertTrue(StreamingServices.isKnown(k)));
        assertFalse(StreamingServices.isKnown("funimation"), "aliases aren't pickable keys");
    }

    @Test
    void linksForCombinesBothSourcesInCatalogOrder() {
        List<StreamingLink> links = List.of(
                new StreamingLink("Netflix", "https://netflix.com/jp", null, null, "Japanese"),
                new StreamingLink("Crunchyroll", "https://crunchyroll.com/x", null, null, null),
                new StreamingLink("Netflix", "https://netflix.com/en", null, null, "English"),
                new StreamingLink("Laftel", "https://laftel.net/x", null, null, null));
        var result = StreamingServices.linksFor(links, List.of(
                "{\"netflix\":\"https://netflix.com/title/1\",\"hulu\":\"https://hulu.com/x\"}",
                "not json", "{\"crunchyroll\":\"https://other\"}"));

        assertEquals(List.of(
                new StreamingServices.ServiceLink("crunchyroll", "https://crunchyroll.com/x"),
                new StreamingServices.ServiceLink("netflix", "https://netflix.com/en"),
                new StreamingServices.ServiceLink("hulu", "https://hulu.com/x")), result);
    }

    @Test
    void linksForShowOnlyOnUnlistedSitesIsEmpty() {
        assertEquals(List.of(), StreamingServices.linksFor(
                List.of(new StreamingLink("Laftel", "https://laftel.net/x", null, null, null)), null));
    }

    @Test
    void catalogCarriesAliasesForTheFrontend() {
        var amazon = StreamingServices.all().stream().filter(s -> s.key().equals("amazon")).findFirst().orElseThrow();
        assertTrue(amazon.aliases().contains("Amazon Prime Video"));
    }
}
