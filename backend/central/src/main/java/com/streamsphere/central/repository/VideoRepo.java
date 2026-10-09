package com.streamsphere.central.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.Video;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoRepo extends JpaRepository<Video, String>{
    List<Video> findAllByOrderByUploadDateTimeDesc();

    // Only fetch PUBLISHED videos
    @Query("SELECT v FROM Video v WHERE v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC' ORDER BY v.uploadDateTime DESC")
    Page<Video> findFeedVideos(Pageable pageable);

    // Search query across Video name, description, and tags, or channel name
    @Query("SELECT DISTINCT v FROM Video v " +
           "LEFT JOIN v.tags t " +
           "JOIN v.channel c " +
           "WHERE (v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC') AND " +
           "(LOWER(v.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(v.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Video> searchVideos(@Param("query") String query, Pageable pageable);

    // Trending: Videos from the last 7 days sorted by views
    @Query("SELECT v FROM Video v " +
           "WHERE v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC' AND " +
           "v.uploadDateTime >= :since " +
           "ORDER BY v.views DESC")
    Page<Video> findTrendingVideos(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT v FROM Video v JOIN v.channel c JOIN c.subscribers s " +
           "WHERE s.id = :userId AND v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC' " +
           "ORDER BY v.uploadDateTime DESC")
    Page<Video> findSubscribedVideos(@Param("userId") java.util.UUID userId, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC' AND v.isShort = true ORDER BY FUNCTION('RANDOM')")
    Page<Video> findShorts(Pageable pageable);
}
