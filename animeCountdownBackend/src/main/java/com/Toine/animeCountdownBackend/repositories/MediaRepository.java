package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
    List<MediaEntity> findByDayIgnoreCase(String day);
    Page<MediaEntity> findAllBy(Pageable pageable);
    @Query("SELECT m FROM MediaEntity m ORDER BY m.next_Airing_At ASC")
    Page<MediaEntity> findAllByOrderByNext_Airing_AtAsc(Pageable pageable);

    @Query("SELECT e FROM MediaEntity e WHERE " +
            "LOWER(e.title_Romaji) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.title_English) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<MediaEntity> searchByFields(@Param("searchTerm") String searchTerm);

    Page<MediaEntity> findAllByOrderByPopularityDesc(Pageable pageable);
}
