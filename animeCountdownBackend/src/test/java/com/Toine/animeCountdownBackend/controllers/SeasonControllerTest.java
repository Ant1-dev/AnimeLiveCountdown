package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.discovery.SeasonKey;
import com.Toine.animeCountdownBackend.models.discovery.SeasonResponse;
import com.Toine.animeCountdownBackend.models.discovery.SeasonSort;
import com.Toine.animeCountdownBackend.services.discovery.SeasonService;
import com.Toine.animeCountdownBackend.services.discovery.SeasonUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeasonControllerTest {
    private SeasonService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SeasonService.class);
        when(service.currentSeason()).thenReturn(new SeasonKey("FALL", 2026));
        mvc = MockMvcBuilders.standaloneSetup(new SeasonController(service)).build();
    }

    @Test
    void defaultsToCurrentSeasonAndPopularity() throws Exception {
        when(service.getSeason(any(), any(), any())).thenReturn(
                new SeasonResponse("FALL", 2026, LocalDate.of(2026, 10, 1), "local", List.of(), List.of()));

        mvc.perform(get("/api/search/season")).andExpect(status().isOk())
                .andExpect(jsonPath("$.season").value("FALL"));

        verify(service).getSeason(new SeasonKey("FALL", 2026), null, SeasonSort.POPULARITY);
    }

    @Test
    void passesFiltersThrough() throws Exception {
        when(service.getSeason(any(), any(), any())).thenReturn(
                new SeasonResponse("WINTER", 2020, LocalDate.of(2020, 1, 1), "anilist", List.of(), List.of()));

        mvc.perform(get("/api/search/season").param("season", "winter").param("year", "2020")
                        .param("genre", "Action").param("sort", "next_airing"))
                .andExpect(status().isOk());

        verify(service).getSeason(new SeasonKey("WINTER", 2020), "Action", SeasonSort.NEXT_AIRING);
    }

    @Test
    void rejectsBadInput() throws Exception {
        mvc.perform(get("/api/search/season").param("season", "autumn").param("year", "2020"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/search/season").param("season", "fall").param("year", "1800"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/search/season").param("sort", "random"))
                .andExpect(status().isBadRequest());
        verify(service, never()).getSeason(any(), any(), any());
    }

    @Test
    void returns503WhenSeasonCantBeFetched() throws Exception {
        when(service.getSeason(eq(new SeasonKey("WINTER", 2001)), any(), any()))
                .thenThrow(new SeasonUnavailableException("busy"));

        mvc.perform(get("/api/search/season").param("season", "WINTER").param("year", "2001"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "120"));
    }
}
