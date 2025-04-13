package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaInfoRepository extends JpaRepository<MediaInfoEntity, Long> {

    @Query("SELECT DISTINCT m FROM MediaInfoEntity m JOIN FETCH m.genres WHERE m.id = :id")
    Optional<MediaInfoEntity> findByIdWithGenres(@Param("id") Long id);

    @Query("SELECT m FROM MediaInfoEntity m WHERE m.airingat IS NOT NULL ORDER BY m.popularity DESC")
    Page<MediaInfoEntity> findAllByPopularityWithAiringDate(Pageable pageable);

    @Query(value = "SELECT * FROM media_info WHERE " +
                   "rom_title ILIKE '%' || :searchTerm || '%' OR " +
                   "eng_title ILIKE '%' || :searchTerm || '%' " +
                   "ORDER BY media_popularity DESC LIMIT 20",
            nativeQuery = true)
    List<MediaInfoEntity> searchByFields(@Param("searchTerm") String searchTerm);

    @Query("SELECT m.id FROM MediaInfoEntity m")
    List<Long> findAllIds();
}
