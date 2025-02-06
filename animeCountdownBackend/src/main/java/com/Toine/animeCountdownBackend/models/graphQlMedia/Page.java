package com.Toine.animeCountdownBackend.models.graphQlMedia;

import java.util.List;

public class Page {
    private List<Media> media;

    public List<Media> getMedia() {
        return media;
    }

    public void setMedia(List<Media> media) {
        this.media = media;
    }
}
