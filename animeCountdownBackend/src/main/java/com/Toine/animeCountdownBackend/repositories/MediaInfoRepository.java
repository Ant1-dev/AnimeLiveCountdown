package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaInfoRepository extends JpaRepository<MediaInfoEntity, Long> {
    @Query("SELECT m.id FROM MediaEntity m")
    List<Long> findAllIds();

    @Query("SELECT m FROM MediaInfoEntity m LEFT JOIN FETCH m.genres WHERE m.id = :id")
    Optional<MediaInfoEntity> findByIdWithGenres(@Param("id") Long id);
}
