package com.Toine.animeCountdownBackend.models.discovery;

import java.time.LocalDate;

public record SeasonOverview(SeasonKey current, SeasonKey next, LocalDate nextStartsAt, int minYear) {
}
