package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.AniListAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AniListAccountRepository extends JpaRepository<AniListAccountEntity, Long> {

    @Query("SELECT a FROM AniListAccountEntity a WHERE a.user.id = :userId")
    Optional<AniListAccountEntity> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AniListAccountEntity a WHERE a.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
