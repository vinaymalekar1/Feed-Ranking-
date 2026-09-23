package com.yourname.feed.fanout;

import com.yourname.feed.cache.FeedCache;
import com.yourname.feed.graph.FollowGraph;
import com.yourname.feed.model.Post;
import com.yourname.feed.ranking.ScoredPost;
import com.yourname.feed.ranking.ScoringFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * When a non-celebrity user creates a post, push it into every follower's
 * precomputed feed (stored in the FeedCache) asynchronously, so those
 * followers' ranked feed reads are just a cache lookup instead of a live
 * fan-out-on-read query.
 *
 * Only called for authors below the celebrity follower-count threshold -
 * see PostService and FanOutOnReadService for the celebrity path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FanOutOnWriteService {

    private final FollowGraph followGraph;
    private final FeedCache feedCache;
    private final ScoringFunction scoringFunction;

    @Value("${feed.cache.max-size-per-user:500}")
    private int maxCacheSizePerUser;

    @Async("feedTaskExecutor")
    public void fanOut(Post post) {
        Set<Long> followerIds = followGraph.getFollowers(post.getAuthorId());
        if (followerIds.isEmpty()) {
            return;
        }

        for (Long followerId : followerIds) {
            double affinity = scoringFunction.placeholderAffinity(followerId, post.getAuthorId());
            double score = scoringFunction.score(post, affinity);
            feedCache.upsert(followerId, new ScoredPost(post, score), maxCacheSizePerUser);
        }

        log.debug("Fanned out post {} from author {} to {} followers",
                post.getId(), post.getAuthorId(), followerIds.size());
    }
}
