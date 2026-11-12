package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationSettingsEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.NotificationSettingsRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** The per-user secret calendar feed: token management and building the ICS document. */
@Service
public class CalendarFeedService {
    private static final SecureRandom RANDOM = new SecureRandom();
    // Keep an episode in the feed for a while after it starts so it doesn't vanish mid-watch
    private static final Duration KEEP_AFTER_START = Duration.ofHours(6);

    private final NotificationSettingsRepository settingsRepository;
    private final FavoriteMediaRepository favoriteMediaRepository;
    private final MediaInfoRepository mediaInfoRepository;
    private final UserRepository userRepository;
    private final IcsCalendarWriter writer;

    public CalendarFeedService(NotificationSettingsRepository settingsRepository,
                               FavoriteMediaRepository favoriteMediaRepository,
                               MediaInfoRepository mediaInfoRepository,
                               UserRepository userRepository,
                               @Value("${app.frontend.url:https://animelivecountdown.com}") String frontendUrl) {
        this.settingsRepository = settingsRepository;
        this.favoriteMediaRepository = favoriteMediaRepository;
        this.mediaInfoRepository = mediaInfoRepository;
        this.userRepository = userRepository;
        this.writer = new IcsCalendarWriter(frontendUrl, "animelivecountdown.com");
    }

    /** Creates a new token (replacing any old one, which stops working) and returns it. */
    @Transactional
    public String regenerateToken(Long userId) {
        NotificationSettingsEntity settings = settingsFor(userId);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        settings.setCalendarToken(token);
        settings.setCalendarTokenCreatedAt(Instant.now());
        settingsRepository.save(settings);
        return token;
    }

    @Transactional
    public void disable(Long userId) {
        settingsRepository.findById(userId).ifPresent(settings -> {
            settings.setCalendarToken(null);
            settings.setCalendarTokenCreatedAt(null);
            settingsRepository.save(settings);
        });
    }

    /** The ICS feed for the user the token belongs to, or empty if the token is unknown. */
    @Transactional(readOnly = true)
    public Optional<String> feedForToken(String token, Instant now) {
        if (token == null || token.length() < 32 || token.length() > 64) {
            return Optional.empty();
        }
        return settingsRepository.findByCalendarToken(token)
                .map(settings -> writer.write(episodesFor(settings.getUserId(), now), now));
    }

    List<IcsCalendarWriter.Episode> episodesFor(Long userId, Instant now) {
        Instant cutoff = now.minus(KEEP_AFTER_START);
        List<MediaEntity> shows = favoriteMediaRepository.findAllFavMedia(userId).stream()
                .filter(m -> m.getNext_Airing_At() != null && m.getNext_Airing_Episode() != null)
                .filter(m -> m.getNext_Airing_At().isAfter(cutoff))
                .toList();
        Map<Long, Integer> durations = mediaInfoRepository.findAllById(shows.stream().map(MediaEntity::getId).toList())
                .stream()
                .filter(info -> info.getDuration() != null)
                .collect(Collectors.toMap(MediaInfoEntity::getId, MediaInfoEntity::getDuration, (a, b) -> a));
        return shows.stream()
                .map(m -> new IcsCalendarWriter.Episode(m.getId(), m.getNext_Airing_Episode(), displayTitle(m),
                        m.getNext_Airing_At(), durations.getOrDefault(m.getId(), 24)))
                .toList();
    }

    public NotificationSettingsEntity settingsFor(Long userId) {
        return settingsRepository.findById(userId).orElseGet(() -> {
            NotificationSettingsEntity created = new NotificationSettingsEntity();
            UserEntity user = userRepository.getReferenceById(userId);
            created.setUser(user);
            return created;
        });
    }

    static String displayTitle(MediaEntity m) {
        if (m.getTitle_English() != null && !m.getTitle_English().isBlank()) {
            return m.getTitle_English();
        }
        return m.getTitle_Romaji() != null ? m.getTitle_Romaji() : "Unknown title";
    }
}
