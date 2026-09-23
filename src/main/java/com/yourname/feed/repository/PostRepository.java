package com.yourname.feed.repository;

import com.yourname.feed.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Used by the NAIVE feed endpoint: posts from a set of followed authors,
     * ordered purely by recency. Backed by idx_posts_author_created.
     */
    @Query("SELECT p FROM Post p WHERE p.authorId IN :authorIds ORDER BY p.createdAt DESC")
    List<Post> findRecentByAuthorIds(@Param("authorIds") List<Long> authorIds, Pageable pageable);

    /**
     * Used by the fan-out-on-read path to live-pull recent posts from a single
     * "celebrity" author (one whose follower count exceeds the configured
     * threshold, so we don't fan their posts out to every follower on write).
     */
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}
