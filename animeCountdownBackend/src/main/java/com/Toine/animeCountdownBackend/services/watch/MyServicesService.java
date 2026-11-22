package com.Toine.animeCountdownBackend.services.watch;

import com.Toine.animeCountdownBackend.models.discovery.StreamingLink;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserStreamingServicesEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.repositories.UserStreamingServicesRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Saved "My services" per user, and which services carry which shows. */
@Service
public class MyServicesService {
    /** Release rows this old still say which platforms carry a show. */
    static final Duration STREAMS_LOOKBACK = Duration.ofDays(30);
    public static final int MAX_AVAILABILITY_IDS = 300;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final UserStreamingServicesRepository repository;
    private final MediaInfoRepository mediaInfo;
    private final ReleaseTimeRepository releases;
    private final EntityManager entityManager;
    private final Clock clock;

    @Autowired
    public MyServicesService(UserStreamingServicesRepository repository, MediaInfoRepository mediaInfo,
                             ReleaseTimeRepository releases, EntityManager entityManager) {
        this(repository, mediaInfo, releases, entityManager, Clock.systemUTC());
    }

    MyServicesService(UserStreamingServicesRepository repository, MediaInfoRepository mediaInfo,
                      ReleaseTimeRepository releases, EntityManager entityManager, Clock clock) {
        this.repository = repository;
        this.mediaInfo = mediaInfo;
        this.releases = releases;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MyServices.Selection get(Long userId) {
        return repository.findById(userId).map(MyServicesService::toSelection).orElse(MyServices.Selection.empty());
    }

    /** @throws MyServices.InvalidSelectionException when the request breaks a rule */
    @Transactional
    public MyServices.Selection save(UserEntity user, MyServices.Selection request) {
        MyServices.Selection clean = MyServices.validate(request);
        UserStreamingServicesEntity entity = repository.findById(user.getId()).orElseGet(() -> {
            UserStreamingServicesEntity created = new UserStreamingServicesEntity();
            created.setUser(entityManager.getReference(UserEntity.class, user.getId()));
            return created;
        });
        entity.setServicesJson(write(clean.services()));
        entity.setCustomPlatformsJson(write(clean.customPlatforms()));
        entity.setUpdatedAt(clock.instant());
        repository.save(entity);
        return clean;
    }

    /**
     * Media id -> service links for each show that has any streaming data. Shows without data are left
     * out (the site labels them "unknown"); an empty list means it streams only outside the catalog.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<StreamingServices.ServiceLink>> availability(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<StreamingLink>> links = new HashMap<>();
        for (Object[] row : mediaInfo.findStreamingLinksByIds(mediaIds)) {
            @SuppressWarnings("unchecked")
            List<StreamingLink> list = (List<StreamingLink>) row[1];
            if (list != null && !list.isEmpty()) {
                links.put((Long) row[0], list);
            }
        }
        Map<Long, List<String>> streams = new HashMap<>();
        Instant since = clock.instant().minus(STREAMS_LOOKBACK);
        for (Object[] row : releases.findStreamsByMediaIds(mediaIds, since)) {
            String json = (String) row[1];
            if (!StreamingServices.streams(json).isEmpty()) {
                streams.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add(json);
            }
        }
        Map<Long, List<StreamingServices.ServiceLink>> out = new TreeMap<>();
        Set<Long> withData = new HashSet<>(links.keySet());
        withData.addAll(streams.keySet());
        for (Long id : withData) {
            out.put(id, StreamingServices.linksFor(links.get(id), streams.get(id)));
        }
        return out;
    }

    static MyServices.Selection toSelection(UserStreamingServicesEntity entity) {
        List<String> services = read(entity.getServicesJson(), new TypeReference<List<String>>() { });
        List<MyServices.CustomPlatform> customs = read(entity.getCustomPlatformsJson(),
                new TypeReference<List<MyServices.CustomPlatform>>() { });
        // Keys removed from the catalog since they were saved are dropped quietly
        return new MyServices.Selection(services.stream().filter(StreamingServices::isKnown).toList(), customs);
    }

    private static <T> List<T> read(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<T> list = JSON.readValue(json, type);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize my services", e);
        }
    }
}
