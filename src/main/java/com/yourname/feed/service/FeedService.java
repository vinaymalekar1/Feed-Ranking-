package com.yourname.feed.service;

import com.yourname.feed.cache.FeedCache;
import com.yourname.feed.dto.FeedItemDto;
import com.yourname.feed.dto.FeedResponseDto;
import com.yourname.feed.fanout.FanOutOnReadService;
import com.yourname.feed.graph.FollowGraph;
import com.yourname.feed.model.Post;
import com.yourname.feed.ranking.RankingEngine;
import com.yourname.feed.ranking.ScoredPost;
import com.yourname.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds both feed variants for a user:
 *
 *  - getNaiveFeed:  a single SQL query across all followed authors, sorted
 *                    purely by createdAt DESC. No scoring, no cache.
 *
 *  - getRankedFeed: merges two candidate sources and re-ranks them together
 *                    with RankingEngine:
 *                      1. the precomputed fan-out-on-write cache (posts from
 *                         non-celebrity authors, pushed in on post creation)
 *                      2. a live fan-out-on-read pull for any celebrity
 *                         authors the user follows (never cached)
 *                    If the cache is cold for this user (e.g. right after
 *                    /api/seed, before any new posts trigger fan-out), it
 *                    falls back to pulling recent posts from all followed
 *                    non-celebrity authors directly from the DB so the
 *                    ranked feed still works end-to-end for seeded data.
 */
@Service
@RequiredArgsConstructor
public class FeedService {

    private final FollowGraph followGraph;
    private final FeedCache feedCache;
    private final PostRepository postRepository;
    private final RankingEngine rankingEngine;
    private final FanOutOnReadService fanOutOnReadService;

    @Value("${feed.celebrity-threshold:10000}")
    private int celebrityThreshold;

    @Value("${feed.ranking.top-n:20}")
    private int defaultTopN;

    public FeedResponseDto getNaiveFeed(Long userId, int limit) {
        Set<Long> following = followGraph.getFollowing(userId);

        List<FeedItemDto> items;
        if (following.isEmpty()) {
            items = List.of();
        } else {
            List<Post> posts = postRepository.findRecentByAuthorIds(
                    new ArrayList<>(following), PageRequest.of(0, limit));
            items = posts.stream().map(this::toFeedItem).toList();
        }

        return FeedResponseDto.builder()
                .userId(userId)
                .feedType("naive")
                .count(items.size())
                .items(items)
                .build();
    }

    public FeedResponseDto getRankedFeed(Long userId, int limit) {
        int topN = limit > 0 ? limit : defaultTopN;
        Set<Long> following = followGraph.getFollowing(userId);

        List<Post> candidatePosts = new ArrayList<>();

        // 1. Precomputed fan-out-on-write cache (non-celebrity authors).
        List<ScoredPost> cached = feedCache.get(userId);
        if (!cached.isEmpty()) {
            for (ScoredPost scoredPost : cached) {
                candidatePosts.add(scoredPost.getPost());
            }
        } else if (!following.isEmpty()) {
            // Cold-start fallback: cache hasn't been populated yet for this
            // user (e.g. right after /api/seed). Pull recent posts directly
            // from non-celebrity followed authors so the ranked feed still
            // returns something sensible instead of an empty list.
            List<Long> nonCelebrityFollowing = following.stream()
                    .filter(id -> !followGraph.isCelebrity(id, celebrityThreshold))
                    .toList();
            if (!nonCelebrityFollowing.isEmpty()) {
                candidatePosts.addAll(postRepository.findRecentByAuthorIds(
                        nonCelebrityFollowing, PageRequest.of(0, Math.max(topN * 5, 100))));
            }
        }

        // 2. Fan-out-on-read: live pull for any celebrities the user follows.
        candidatePosts.addAll(fanOutOnReadService.pullLiveForCelebrities(userId));

        // Dedupe by post id (a post could theoretically appear in both sources).
        Map<Long, Post> deduped = new LinkedHashMap<>();
        for (Post post : candidatePosts) {
            deduped.putIfAbsent(post.getId(), post);
        }

        List<ScoredPost> ranked = rankingEngine.rankTopN(new ArrayList<>(deduped.values()), userId, topN);
        List<FeedItemDto> items = ranked.stream().map(this::toFeedItem).toList();

        return FeedResponseDto.builder()
                .userId(userId)
                .feedType("ranked")
                .count(items.size())
                .items(items)
                .build();
    }

    private FeedItemDto toFeedItem(Post post) {
        return FeedItemDto.builder()
                .postId(post.getId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .likes(post.getLikes())
                .comments(post.getComments())
                .createdAt(post.getCreatedAt())
                .score(null)
                .build();
    }

    private FeedItemDto toFeedItem(ScoredPost scoredPost) {
        Post post = scoredPost.getPost();
        return FeedItemDto.builder()
                .postId(post.getId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .likes(post.getLikes())
                .comments(post.getComments())
                .createdAt(post.getCreatedAt())
                .score(scoredPost.getScore())
                .build();
    }
}
