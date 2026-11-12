package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.dto.FollowView;
import com.Toine.animeCountdownBackend.services.AiringDayWindows;
import com.Toine.animeCountdownBackend.services.FollowingService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

/**
 * The signed-in user's followed shows ("Following"), episode progress and My Schedule.
 * The user always comes from the access token, never from the URL, so nobody can read or change
 * another user's follows. /api/auth/favorite keeps working for older clients.
 */
@RestController
@RequestMapping("/api/auth/following")
@Validated
public class FollowingController {
    private final FollowingService service;

    public FollowingController(FollowingService service) {
        this.service = service;
    }

    public record ProgressRequest(@NotNull Integer watchedEpisode) {
    }

    @GetMapping
    public ResponseEntity<List<FollowView>> list() {
        return noStore(service.list(CurrentUser.require().getId()));
    }

    @PostMapping("/{mediaId}")
    public FollowView follow(@PathVariable @Positive Long mediaId) {
        return service.follow(CurrentUser.require().getId(), mediaId);
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> unfollow(@PathVariable @Positive Long mediaId) {
        service.unfollow(CurrentUser.require().getId(), mediaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{mediaId}/progress")
    public FollowView setProgress(@PathVariable @Positive Long mediaId,
                                  @RequestBody @Validated ProgressRequest request) {
        return service.setProgress(CurrentUser.require().getId(), mediaId, request.watchedEpisode());
    }

    /** My Schedule: followed shows airing on this weekday in the viewer's timezone. */
    @GetMapping("/airing/{weekDay}")
    public ResponseEntity<List<FollowView>> airingOn(
            @PathVariable
            @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$",
                     message = "Invalid day of week")
            String weekDay,
            @RequestParam(name = "tz", required = false) @Size(max = 64) String timeZone) {
        return noStore(service.airingOn(CurrentUser.require().getId(), DayOfWeek.valueOf(weekDay),
                AiringDayWindows.resolveZone(timeZone)));
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
