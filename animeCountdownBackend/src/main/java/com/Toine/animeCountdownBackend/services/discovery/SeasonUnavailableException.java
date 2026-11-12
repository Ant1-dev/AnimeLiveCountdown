package com.Toine.animeCountdownBackend.services.discovery;

/** A past season isn't cached and can't be fetched right now (budget used up or AniList failed). */
public class SeasonUnavailableException extends RuntimeException {
    public SeasonUnavailableException(String message) {
        super(message);
    }

    public SeasonUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
