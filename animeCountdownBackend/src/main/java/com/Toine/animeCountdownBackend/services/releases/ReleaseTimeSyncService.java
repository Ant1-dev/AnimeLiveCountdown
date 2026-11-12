package com.Toine.animeCountdownBackend.services.releases;

import com.Toine.animeCountdownBackend.models.postgreEntities.AnimeScheduleLinkEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.Toine.animeCountdownBackend.repositories.AnimeScheduleLinkRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.AnimeDetails;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.TimetableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.IsoFields;
import java.util.*;

/**
 * Hourly sync of subbed and dubbed release times from AnimeSchedule.net into release_time.
 *
 * Fetches this week's and next week's sub and dub timetables (4 requests), maps each AnimeSchedule
 * show to its AniList id (one lookup per new show, then cached in animeschedule_link), and upserts
 * one row per show/type/episode. Off until ANIMESCHEDULE_TOKEN is set.
 */
@Service
public class ReleaseTimeSyncService {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseTimeSyncService.class);
    /** New shows looked up per run; the rest are picked up next hour. */
    private static final int MAX_LOOKUPS_PER_RUN = 60;
    private static final Duration KEEP = Duration.ofDays(14);

    private final AnimeScheduleClient client;
    private final ReleaseTimeRepository releases;
    private final AnimeScheduleLinkRepository links;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ReleaseTimeSyncService(AnimeScheduleClient client, ReleaseTimeRepository releases, AnimeScheduleLinkRepository links) {
        this(client, releases, links, Clock.systemUTC());
    }

    ReleaseTimeSyncService(AnimeScheduleClient client, ReleaseTimeRepository releases, AnimeScheduleLinkRepository links, Clock clock) {
        this.client = client;
        this.releases = releases;
        this.links = links;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "${app.sync.release-times.initial-delay:PT3M}",
               fixedDelayString = "${app.sync.release-times.interval:PT1H}")
    public void sync() {
        if (!client.isEnabled()) {
            return;
        }
        try {
            Instant now = clock.instant();
            ZonedDateTime nextWeek = now.atZone(ZoneOffset.UTC).plusWeeks(1);
            List<TimetableEntry> entries = new ArrayList<>();
            for (String airType : List.of("sub", "dub")) {
                entries.addAll(client.timetable(airType, null, null));
                entries.addAll(client.timetable(airType, nextWeek.get(IsoFields.WEEK_BASED_YEAR), nextWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
            }
            int saved = apply(entries, now);
            int pruned = releases.deleteOlderThan(now.minus(KEEP));
            logger.info("Release times: {} timetable entries, {} saved, {} old rows removed", entries.size(), saved, pruned);
        } catch (RuntimeException e) {
            logger.warn("Release time sync failed: {}", e.getMessage());
        }
    }

    int apply(List<TimetableEntry> entries, Instant now) {
        Map<String, Long> aniListIds = resolveRoutes(entries, now);
        int saved = 0;
        for (TimetableEntry e : entries) {
            Long mediaId = aniListIds.get(e.route());
            Optional<String> type = ReleaseTimes.airType(e.airType());
            Optional<Instant> at = ReleaseTimes.instant(e.episodeDate());
            if (mediaId == null || type.isEmpty() || at.isEmpty() || e.episodeNumber() == null || e.episodeNumber() < 1) {
                continue;
            }
            ReleaseTimeEntity row = releases.findByMediaIdAndAirTypeAndEpisode(mediaId, type.get(), e.episodeNumber())
                    .orElseGet(ReleaseTimeEntity::new);
            row.setMediaId(mediaId);
            row.setAirType(type.get());
            row.setEpisode(e.episodeNumber());
            row.setReleaseAt(at.get());
            row.setStreamsJson(ReleaseTimes.streamsToJson(ReleaseTimes.normalizeStreams(e.streams())));
            row.setDelayedUntil(ReleaseTimes.instant(e.delayedUntil()).orElse(null));
            row.setDelayNote(e.delayedText() == null || e.delayedText().isBlank() ? null
                    : e.delayedText().substring(0, Math.min(255, e.delayedText().length())));
            row.setUpdatedAt(now);
            releases.save(row);
            saved++;
        }
        return saved;
    }

    /** AnimeSchedule route -> AniList id, from the cache or (for new shows) one lookup each. */
    private Map<String, Long> resolveRoutes(List<TimetableEntry> entries, Instant now) {
        Set<String> routes = new LinkedHashSet<>();
        entries.forEach(e -> {
            if (e.route() != null && !e.route().isBlank()) {
                routes.add(e.route());
            }
        });
        Map<String, Long> ids = new HashMap<>();
        Map<String, AnimeScheduleLinkEntity> known = new HashMap<>();
        links.findAllById(routes).forEach(l -> known.put(l.getRoute(), l));
        int lookups = 0;
        for (String route : routes) {
            AnimeScheduleLinkEntity link = known.get(route);
            if (link == null) {
                if (lookups++ >= MAX_LOOKUPS_PER_RUN) {
                    continue;
                }
                try {
                    AnimeDetails details = client.anime(route);
                    Long id = details == null ? null : ReleaseTimes.aniListId(details.websites()).orElse(null);
                    link = links.save(new AnimeScheduleLinkEntity(route, id, now));
                } catch (RuntimeException ex) {
                    logger.debug("AnimeSchedule lookup failed for {}: {}", route, ex.getMessage());
                    continue;
                }
            }
            if (link.getAniListId() != null) {
                ids.put(route, link.getAniListId());
            }
        }
        return ids;
    }
}
