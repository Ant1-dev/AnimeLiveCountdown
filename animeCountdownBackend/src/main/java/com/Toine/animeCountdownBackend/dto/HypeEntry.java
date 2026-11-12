package com.Toine.animeCountdownBackend.dto;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;

/** One show in the Most Hyped ranking. */
public record HypeEntry(MediaEntity media, long score, long recentReactions, long newFollowers, long followers) {
}
