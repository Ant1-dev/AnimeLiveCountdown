package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserStreamingServicesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStreamingServicesRepository extends JpaRepository<UserStreamingServicesEntity, Long> {
}
