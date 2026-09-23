package com.yourname.feed.ranking;

import com.yourname.feed.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingEngineTest {

    private RankingEngine rankingEngine;

    @BeforeEach
    void setUp() {
        // Fixed weights so scores are deterministic and easy to reason about.
        ScoringFunction scoringFunction = new ScoringFunction(0.08, 0.6, 1.2, 5.0);
        rankingEngine = new RankingEngine(scoringFunction);
    }

    private Post post(Long id, Long authorId, int likes, int comments, int hoursAgo) {
        return Post.builder()
                .id(id)
                .authorId(authorId)
                .content("content " + id)
                .likes(likes)
                .comments(comments)
                .createdAt(LocalDateTime.now().minusHours(hoursAgo))
                .build();
    }

    @Test
    void returnsEmptyListForNoPosts() {
        List<ScoredPost> result = rankingEngine.rankTopN(new ArrayList<>(), 1L, 20);
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsAllPostsWhenFewerThanTopN() {
        List<Post> posts = List.of(
                post(1L, 100L, 10, 2, 1),
                post(2L, 100L, 5, 1, 2)
        );
        List<ScoredPost> result = rankingEngine.rankTopN(posts, 1L, 20);
        assertEquals(2, result.size());
    }

    @Test
    void resultIsSortedByScoreDescending() {
        List<Post> posts = List.of(
                post(1L, 100L, 1, 0, 1),     // low engagement
                post(2L, 100L, 500, 100, 1), // high engagement, same recency
                post(3L, 100L, 50, 10, 1)    // medium engagement
        );
        List<ScoredPost> result = rankingEngine.rankTopN(posts, 1L, 20);

        assertEquals(3, result.size());
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getScore() >= result.get(i + 1).getScore(),
                    "Results must be sorted by score descending");
        }
        // The highest-engagement post should be ranked first.
        assertEquals(2L, result.get(0).getPost().getId());
    }

    @Test
    void limitsResultsToTopN() {
        List<Post> posts = new ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            posts.add(post(i, 100L, (int) i, 0, 1));
        }
        List<ScoredPost> result = rankingEngine.rankTopN(posts, 1L, 20);
        assertEquals(20, result.size());
        // The 20 highest-like posts (31..50) should be the ones kept.
        assertEquals(50L, result.get(0).getPost().getId());
    }

    @Test
    void recentPostOutranksOlderPostWithSimilarEngagementDueToDecay() {
        Post fresh = post(1L, 100L, 20, 5, 1);
        Post stale = post(2L, 100L, 20, 5, 200); // same engagement, ~8 days old
        List<ScoredPost> result = rankingEngine.rankTopN(List.of(fresh, stale), 1L, 20);

        assertEquals(1L, result.get(0).getPost().getId());
    }
}
