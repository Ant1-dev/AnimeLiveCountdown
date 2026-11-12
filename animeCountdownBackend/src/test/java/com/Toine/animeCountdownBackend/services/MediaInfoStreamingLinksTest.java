package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.discovery.StreamingLink;
import com.Toine.animeCountdownBackend.models.discovery.StreamingLinksConverter;
import com.Toine.animeCountdownBackend.models.graphQlMedia.ExternalLink;
import com.Toine.animeCountdownBackend.models.graphQlMedia.MediaInfo;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MediaInfoStreamingLinksTest {

    @Test
    void keepsOnlyEnabledHttpsStreamingLinks() {
        List<StreamingLink> links = MediaInfoService.toStreamingLinks(List.of(
                new ExternalLink("Crunchyroll", "https://www.crunchyroll.com/x", "icon.png", "#F88B24", null, "STREAMING", false),
                new ExternalLink("Twitter", "https://twitter.com/x", null, null, "Japanese", "SOCIAL", false),
                new ExternalLink("Old site", "https://old.example/x", null, null, null, "STREAMING", true),
                new ExternalLink("Sketchy", "javascript:alert(1)", null, null, null, "STREAMING", false)));

        assertEquals(List.of(new StreamingLink("Crunchyroll", "https://www.crunchyroll.com/x", "icon.png", "#F88B24", null)), links);
        assertEquals(List.of(), MediaInfoService.toStreamingLinks(null));
    }

    @Test
    void convertToEntityReadsExternalLinksFromAniListJson() throws Exception {
        String json = """
                {"id": 5, "title": {"english": "E", "romaji": "R"}, "coverImage": {"extraLarge": "c"},
                 "externalLinks": [{"site": "Netflix", "url": "https://www.netflix.com/title/1", "icon": null,
                                    "color": "#E50914", "language": null, "type": "STREAMING", "isDisabled": false}]}
                """;
        MediaInfo info = new ObjectMapper().readValue(json, MediaInfo.class);
        MediaInfoEntity entity = new MediaInfoService(null, null, null).convertToEntity(info);

        assertEquals(1, entity.getStreamingLinks().size());
        assertEquals("Netflix", entity.getStreamingLinks().get(0).site());
    }

    @Test
    void converterRoundTripsLinks() {
        StreamingLinksConverter converter = new StreamingLinksConverter();
        List<StreamingLink> links = List.of(new StreamingLink("HIDIVE", "https://hidive.com/x", null, "#00AEEF", "English"));

        assertEquals(links, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(links)));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals(List.of(), converter.convertToEntityAttribute("not json"));
    }
}
