package com.Toine.animeCountdownBackend.models;

import java.util.Arrays;
import java.util.Optional;

/** The fixed set of reactions a user can leave on an episode. */
public enum ReactionType {
    FIRE,
    LOVE,
    LOL,
    SHOCK,
    SAD,
    MEH;

    /** Case-insensitive lookup; empty for null or unknown names. */
    public static Optional<ReactionType> parse(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }
}
