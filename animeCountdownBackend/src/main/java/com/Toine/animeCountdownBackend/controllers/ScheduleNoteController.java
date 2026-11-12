package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.ScheduleNote;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Break/delay note for one show. The schedule endpoints already include it on each show; this is for
 * the media info page, which reads media_info rather than media.
 */
@RestController
@RequestMapping("/api/airing/note")
public class ScheduleNoteController {
    private final MediaRepository mediaRepository;

    public ScheduleNoteController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    /** 200 with the active note, or 204 when the show is on schedule or unknown. */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleNote> getNote(@PathVariable Long id) {
        ScheduleNote note = mediaRepository.findById(id)
                .map(media -> ScheduleNote.activeOf(media, Instant.now()))
                .orElse(null);
        if (note == null) {
            return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(note);
    }
}
