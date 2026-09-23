package com.yourname.feed.fanout;

import com.yourname.feed.graph.FollowGraph;
import com.yourname.feed.model.Post;
import com.yourname.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fallback for "celebrity" authors (follower count above the configured
 * threshold): instead of fanning their posts out to every follower's cache
 * on write (which would be extremely expensive at scale), their recent
 * posts are pulled live from the DB at read time and merged into the
 * requester's feed.
 */
@Service
@RequiredArgsConstructor
public class FanOutOnReadService {

    private final FollowGraph followGraph;
    private final PostRepository postRepository;

    @Value("${feed.celebrity-threshold:10000}")
    private int celebrityThreshold;

    @Value("${feed.ranking.top-n:20}")
    private int defaultTopN;

    /**
     * For every celebrity that `viewerId` follows, live-pull their most
     * recent posts (bounded per author so one prolific celebrity can't
     * dominate the candidate set).
     */
    public List<Post> pullLiveForCelebrities(Long viewerId) {
        Set<Long> following = followGraph.getFollowing(viewerId);
        if (following.isEmpty()) {
            return List.of();
        }

        Pageable perAuthorLimit = PageRequest.of(0, defaultTopN);
        List<Post> livePosts = new ArrayList<>();

        for (Long followedId : following) {
            if (followGraph.isCelebrity(followedId, celebrityThreshold)) {
                livePosts.addAll(postRepository.findByAuthorIdOrderByCreatedAtDesc(followedId, perAuthorLimit));
            }
        }

        return livePosts;
    }

    public int getCelebrityThreshold() {
        return celebrityThreshold;
    }
}
