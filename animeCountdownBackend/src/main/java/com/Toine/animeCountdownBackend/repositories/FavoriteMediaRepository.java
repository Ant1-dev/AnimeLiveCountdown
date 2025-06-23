package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
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
public interface FavoriteMediaRepository extends JpaRepository<FavoriteMediaEntity, Long> {

    @Query("SELECT fm.media FROM FavoriteMediaEntity fm WHERE fm.user.id = :userId")
    List<MediaEntity> findAllFavMedia(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FavoriteMediaEntity fm WHERE fm.media.id = :favoriteMediaId")
    int removeFavoriteMedia(@Param("mediaId") Long favoriteMediaId);

    @Query("SELECT COUNT(f) > 0 FROM FavoriteMediaEntity f WHERE f.user.id = :userId AND f.media.id = :mediaId")
    boolean existsByUserIdAndMediaId(@Param("userId") Long userId, @Param("mediaId") Long mediaId);
}
