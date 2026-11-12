package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.ReactionType;
import com.Toine.animeCountdownBackend.models.postgreEntities.EpisodeReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeReactionRepository extends JpaRepository<EpisodeReactionEntity, Long> {

    @Query("SELECT r FROM EpisodeReactionEntity r " +
           "WHERE r.user.id = :userId AND r.mediaId = :mediaId AND r.episode = :episode")
    Optional<EpisodeReactionEntity> findByUserAndEpisode(@Param("userId") Long userId,
                                                         @Param("mediaId") Long mediaId,
                                                         @Param("episode") Integer episode);

    /** Reaction counts for one episode. */
    @Query("SELECT r.reaction AS reaction, COUNT(r) AS total FROM EpisodeReactionEntity r " +
           "WHERE r.mediaId = :mediaId AND r.episode = :episode GROUP BY r.reaction")
    List<ReactionCount> countByEpisode(@Param("mediaId") Long mediaId, @Param("episode") Integer episode);

    @Modifying
    @Query("DELETE FROM EpisodeReactionEntity r " +
           "WHERE r.user.id = :userId AND r.mediaId = :mediaId AND r.episode = :episode")
    int deleteByUserAndEpisode(@Param("userId") Long userId,
                               @Param("mediaId") Long mediaId,
                               @Param("episode") Integer episode);

    /** Reactions left or changed since the given time, per show. */
    @Query("SELECT r.mediaId AS mediaId, COUNT(r) AS total FROM EpisodeReactionEntity r " +
           "WHERE r.updatedAt >= :since GROUP BY r.mediaId")
    List<MediaCount> countRecentByMedia(@Param("since") Instant since);

    interface ReactionCount {
        ReactionType getReaction();
        Long getTotal();
    }

    interface MediaCount {
        Long getMediaId();
        Long getTotal();
    }
}
