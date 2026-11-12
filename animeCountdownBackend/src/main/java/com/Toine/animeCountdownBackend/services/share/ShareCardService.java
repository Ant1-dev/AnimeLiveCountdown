package com.Toine.animeCountdownBackend.services.share;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/** Builds share-preview data for a show from the synced schedule (media) and details (media_info). */
@Service
public class ShareCardService {

    private static final DateTimeFormatter AIRING_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final MediaRepository mediaRepository;
    private final MediaInfoRepository mediaInfoRepository;
    private final Clock clock;

    @Autowired
    public ShareCardService(MediaRepository mediaRepository, MediaInfoRepository mediaInfoRepository) {
        this(mediaRepository, mediaInfoRepository, Clock.systemUTC());
    }

    ShareCardService(MediaRepository mediaRepository, MediaInfoRepository mediaInfoRepository, Clock clock) {
        this.mediaRepository = mediaRepository;
        this.mediaInfoRepository = mediaInfoRepository;
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    @Transactional(readOnly = true)
    public Optional<ShareCard> find(long mediaId) {
        Optional<MediaEntity> media = mediaRepository.findById(mediaId);
        Optional<MediaInfoEntity> info = mediaInfoRepository.findById(mediaId);
        if (media.isEmpty() && info.isEmpty()) {
            return Optional.empty();
        }

        String title = firstNonBlank(
                info.map(MediaInfoEntity::getEngtitle).orElse(null),
                media.map(MediaEntity::getTitle_English).orElse(null),
                info.map(MediaInfoEntity::getRomtitle).orElse(null),
                media.map(MediaEntity::getTitle_Romaji).orElse(null),
                "Anime #" + mediaId);

        // media is synced every minute, so it has the freshest schedule
        String status = media.map(MediaEntity::getStatus).orElseGet(() -> info.map(MediaInfoEntity::getStatus).orElse(null));
        Integer nextEpisode = media.map(MediaEntity::getNext_Airing_Episode)
                .orElseGet(() -> info.map(MediaInfoEntity::getNextepisode).orElse(null));
        Instant nextAiringAt = media.map(MediaEntity::getNext_Airing_At)
                .orElseGet(() -> info.map(MediaInfoEntity::getAiringat).orElse(null));
        Integer total = info.map(MediaInfoEntity::getTotalepisodes).orElse(null);

        Instant now = clock.instant();
        String countdown = CountdownText.line(status, nextEpisode, nextAiringAt, total, now);
        String airingLine = nextAiringAt != null && nextAiringAt.isAfter(now) ? AIRING_FORMAT.format(nextAiringAt) : null;

        String cover = firstNonBlank(info.map(MediaInfoEntity::getCoverimage).orElse(null),
                media.map(MediaEntity::getCover_Image_Url).orElse(null));
        String banner = firstNonBlank(info.map(MediaInfoEntity::getBanner).orElse(null),
                media.map(MediaEntity::getBanner_Image_Url).orElse(null));

        String synopsis = plainText(info.map(MediaInfoEntity::getDescription).orElse(null));
        return Optional.of(new ShareCard(mediaId, title, countdown, airingLine, synopsis, cover, banner));
    }

    /** Strips the HTML AniList puts in descriptions and collapses whitespace. */
    static String plainText(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]+>", "");
        return HtmlUtils.htmlUnescape(text).replaceAll("\\s+", " ").trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
