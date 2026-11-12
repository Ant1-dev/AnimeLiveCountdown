package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    List<PushSubscriptionEntity> findByUser_Id(Long userId);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    @Modifying
    @Transactional
    @Query("DELETE FROM PushSubscriptionEntity s WHERE s.user.id = :userId AND s.endpoint = :endpoint")
    int deleteByUserAndEndpoint(@Param("userId") Long userId, @Param("endpoint") String endpoint);

    @Modifying
    @Transactional
    @Query("DELETE FROM PushSubscriptionEntity s WHERE s.expirationTime IS NOT NULL AND s.expirationTime < :now")
    int deleteExpired(@Param("now") Instant now);
}
