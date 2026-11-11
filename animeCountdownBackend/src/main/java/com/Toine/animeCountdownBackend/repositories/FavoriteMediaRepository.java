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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FavoriteMediaRepository extends JpaRepository<FavoriteMediaEntity, Long> {

    @Query("SELECT m FROM FavoriteMediaEntity fm JOIN fm.media m WHERE fm.user.id = :userId")
    List<MediaEntity> findAllFavMedia(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FavoriteMediaEntity fm WHERE fm.user.id = :userId AND fm.media.id = :mediaId")
    int removeFavoriteMedia(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    @Query("SELECT DISTINCT fm.media.id FROM FavoriteMediaEntity fm WHERE fm.media.id IN :mediaIds")
    Set<Long> findFavoritedMediaIds(@Param("mediaIds") Collection<Long> mediaIds);

    @Query("SELECT COUNT(f) > 0 FROM FavoriteMediaEntity f WHERE f.user.id = :userId AND f.media.id = :mediaId")
    boolean existsByUserIdAndMediaId(@Param("userId") Long userId, @Param("mediaId") Long mediaId);
}
