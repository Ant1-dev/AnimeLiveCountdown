package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.services.watch.MyServices;
import com.Toine.animeCountdownBackend.services.watch.MyServicesService;
import com.Toine.animeCountdownBackend.services.watch.StreamingServices;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

/**
 * "My services": the streaming services catalog, which services carry which shows (both public), and
 * the signed-in user's saved picks. Signed-out visitors keep their picks in the browser.
 */
@RestController
@RequestMapping("/api")
public class MyServicesController {
    private final MyServicesService service;

    public MyServicesController(MyServicesService service) {
        this.service = service;
    }

    @GetMapping("/streaming-services")
    public ResponseEntity<List<StreamingServices.Service>> catalog() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
                .body(StreamingServices.all());
    }

    /** ?ids=1,2,3 -> {"1": [{"key": "crunchyroll", "url": "https://..."}], ...}; shows with no streaming data are left out. */
    @GetMapping("/streaming-services/availability")
    public ResponseEntity<?> availability(@RequestParam(name = "ids", required = false) List<Long> ids) {
        Set<Long> unique = ids == null ? Set.of() : new LinkedHashSet<>(ids);
        if (unique.size() > MyServicesService.MAX_AVAILABILITY_IDS) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Ask for up to " + MyServicesService.MAX_AVAILABILITY_IDS + " shows at a time"));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(service.availability(unique));
    }

    @GetMapping("/auth/my-services")
    public ResponseEntity<MyServices.Selection> get() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.get(user.getId()));
    }

    @PutMapping("/auth/my-services")
    public ResponseEntity<?> put(@RequestBody MyServices.Selection request) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(service.save(user, request));
        } catch (MyServices.InvalidSelectionException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
