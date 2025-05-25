package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteMediaRepository extends JpaRepository<FavoriteMediaEntity, Long> {

    @Query("SELECT fm.media FROM FavoriteMediaEntity fm WHERE fm.id = :userId")
    List<MediaEntity> findFavMedia(@Param("userId") Long userId);
}
