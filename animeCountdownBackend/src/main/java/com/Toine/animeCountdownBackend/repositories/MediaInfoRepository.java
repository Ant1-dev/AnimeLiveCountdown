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

    @Query("SELECT DISTINCT m FROM MediaInfoEntity m JOIN FETCH m.genres WHERE m.id = :id")
    Optional<MediaInfoEntity> findByIdWithGenres(@Param("id") Long id);

    @Query("SELECT e FROM MediaInfoEntity e WHERE " +
           "LOWER(e.romtitle) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.engtitle) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<MediaInfoEntity> searchByFields(@Param("searchTerm") String searchTerm);
}
