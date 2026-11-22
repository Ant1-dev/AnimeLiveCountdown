package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.services.watch.MyServices;
import com.Toine.animeCountdownBackend.services.watch.MyServices.Selection;
import com.Toine.animeCountdownBackend.services.watch.MyServicesService;
import com.Toine.animeCountdownBackend.services.watch.StreamingServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class MyServicesControllerTest {

    private MyServicesService service;
    private MyServicesController controller;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = mock(MyServicesService.class);
        controller = new MyServicesController(service);
        user = new UserEntity();
        user.setId(42L);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void signIn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void catalogIsPublicAndCached() {
        var response = controller.catalog();
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isEmpty());
        assertTrue(response.getHeaders().getCacheControl().contains("public"));
    }

    @Test
    void signedOutCantReadOrSave() {
        assertEquals(401, controller.get().getStatusCode().value());
        assertEquals(401, controller.put(Selection.empty()).getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test
    void getReturnsTheSignedInUsersPicks() {
        signIn();
        Selection saved = new Selection(List.of("crunchyroll"), List.of());
        when(service.get(42L)).thenReturn(saved);

        assertEquals(saved, controller.get().getBody());
    }

    @Test
    void putSavesAndReturnsTheCleanedSelection() {
        signIn();
        Selection request = new Selection(List.of("netflix", "crunchyroll"), List.of());
        Selection clean = new Selection(List.of("crunchyroll", "netflix"), List.of());
        when(service.save(user, request)).thenReturn(clean);

        ResponseEntity<?> response = controller.put(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(clean, response.getBody());
    }

    @Test
    void invalidPutIs400WithTheReason() {
        signIn();
        when(service.save(any(), any())).thenThrow(new MyServices.InvalidSelectionException("Unknown streaming service: x"));

        ResponseEntity<?> response = controller.put(new Selection(List.of("x"), List.of()));

        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Unknown streaming service: x"), response.getBody());
    }

    @Test
    void availabilityDedupesIdsAndCapsTheBatch() {
        when(service.availability(anyCollection())).thenReturn(Map.of(1L, List.of(new StreamingServices.ServiceLink("crunchyroll", "https://c"))));

        assertEquals(200, controller.availability(List.of(1L, 1L, 2L)).getStatusCode().value());
        verify(service).availability(new LinkedHashSet<>(List.of(1L, 2L)));

        List<Long> tooMany = new ArrayList<>();
        for (long i = 0; i <= MyServicesService.MAX_AVAILABILITY_IDS; i++) {
            tooMany.add(i);
        }
        assertEquals(400, controller.availability(tooMany).getStatusCode().value());
    }
}
