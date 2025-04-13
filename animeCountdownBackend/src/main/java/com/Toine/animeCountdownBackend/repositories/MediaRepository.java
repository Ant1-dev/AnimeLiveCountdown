package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    @Query("SELECT m FROM MediaEntity m WHERE m.next_Airing_At IS NOT NULL AND LOWER(m.day) = LOWER(:day) ORDER BY m.popularity DESC")
    List<MediaEntity> findAiringMediaByDayOrderedByPopularity(@Param("day") String day);

    @Query("SELECT m FROM MediaEntity m WHERE m.seasonYear = EXTRACT(YEAR FROM CURRENT_DATE) AND m.status = 'RELEASING' AND m.next_Airing_At IS NOT NULL ORDER BY m.popularity DESC")
    Page<MediaEntity> findAllByCurrentYear(Pageable pageable);

    @Query("SELECT m FROM MediaEntity m ORDER BY m.next_Airing_At ASC")
    Page<MediaEntity> findAllByOrderByNext_Airing_AtAsc(Pageable pageable);

    @Query("SELECT m.id FROM MediaEntity m")
    List<Long> findAllIds();
}
