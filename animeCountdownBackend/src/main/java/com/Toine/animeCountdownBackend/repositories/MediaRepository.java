package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    @Query("SELECT m FROM MediaEntity m WHERE m.next_Airing_At IS NOT NULL AND m.next_Airing_At <= :bufferEndDate AND LOWER(m.day) = LOWER(:day) AND m.status IN ('RELEASING', 'NOT_YET_RELEASED') ORDER BY m.popularity DESC")
    Page<MediaEntity> findAiringMediaByDayOrderedByPopularity(@Param("day") String day, @Param("bufferEndDate") Instant bufferEndDate, Pageable pageable);

    @Query("SELECT m FROM MediaEntity m WHERE m.seasonYear = EXTRACT(YEAR FROM CURRENT_DATE) AND m.status IN ('RELEASING', 'NOT_YET_RELEASED') AND m.next_Airing_At IS NOT NULL AND m.next_Airing_At <= :bufferEndDate ORDER BY m.popularity DESC")
    Page<MediaEntity> findAllByCurrentYear(@Param("bufferEndDate") Instant bufferEndDate, Pageable pageable);

    @Query("SELECT m FROM MediaEntity m WHERE m.next_Airing_At IS NOT NULL AND m.status IN ('RELEASING', 'NOT_YET_RELEASED') ORDER BY m.next_Airing_At ASC")
    Page<MediaEntity> findAllByOrderByNext_Airing_AtAsc(Pageable pageable);

    @Query("SELECT m.id FROM MediaEntity m")
    List<Long> findAllIds();

}
