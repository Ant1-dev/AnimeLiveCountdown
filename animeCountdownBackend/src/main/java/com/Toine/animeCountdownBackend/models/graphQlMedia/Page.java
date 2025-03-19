package com.Toine.animeCountdownBackend.models.graphQlMedia;

import java.util.List;

public class Page {
    private List<Media> media;
    private List<MediaInfo> mediaInfo;

    public List<MediaInfo> getMediaInfo() { return mediaInfo; }
    public List<Media> getMedia() {
        return media;
    }
}
