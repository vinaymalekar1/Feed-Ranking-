package com.yourname.feed.ranking;

import com.yourname.feed.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Selects the top-N highest-scored posts out of a candidate list without
 * sorting the whole thing: maintains a min-heap of size N, so overall
 * complexity is O(M log N) for M candidate posts instead of O(M log M).
 */
@Component
@RequiredArgsConstructor
public class RankingEngine {

    private final ScoringFunction scoringFunction;

    /**
     * @param posts  candidate posts (already deduplicated by the caller)
     * @param viewerId the user the feed is being generated for (used for affinity)
     * @param topN   how many top posts to return
     * @return posts sorted by score, descending, highest first
     */
    public List<ScoredPost> rankTopN(List<Post> posts, Long viewerId, int topN) {
        if (posts == null || posts.isEmpty() || topN <= 0) {
            return List.of();
        }

        // Min-heap ordered by score ascending: the lowest-scored of the
        // current top-N sits at the head, so we can cheaply evict it when a
        // better candidate comes along.
        PriorityQueue<ScoredPost> minHeap =
                new PriorityQueue<>(topN + 1, Comparator.comparingDouble(ScoredPost::getScore));

        for (Post post : posts) {
            double affinity = scoringFunction.placeholderAffinity(viewerId, post.getAuthorId());
            double score = scoringFunction.score(post, affinity);
            ScoredPost scoredPost = new ScoredPost(post, score);

            if (minHeap.size() < topN) {
                minHeap.offer(scoredPost);
            } else if (minHeap.peek() != null && score > minHeap.peek().getScore()) {
                minHeap.poll();
                minHeap.offer(scoredPost);
            }
        }

        List<ScoredPost> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return result;
    }
}
