package com.yourname.feed.cache;

import com.yourname.feed.ranking.ScoredPost;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe in-memory implementation of {@link FeedCache}.
 *
 * Each user's feed is stored as a ConcurrentSkipListSet<ScoredPost>, sorted
 * highest-score-first, giving O(log n) inserts and cheap trimming to a max
 * size. This is a drop-in stand-in for a v2 Redis-backed cache (e.g. a
 * Redis Sorted Set per user, ZADD/ZREVRANGE/ZREMRANGEBYRANK) - the FeedCache
 * interface is deliberately shaped to make that swap straightforward.
 */
@Component
public class InMemoryFeedCache implements FeedCache {

    // Order by score descending; break ties by post id so posts with an
    // identical score are never treated as "equal" and dropped by the Set.
    private static final Comparator<ScoredPost> SCORE_DESC_COMPARATOR =
            Comparator.comparingDouble(ScoredPost::getScore).reversed()
                    .thenComparing(sp -> sp.getPost().getId());

    private final Map<Long, ConcurrentSkipListSet<ScoredPost>> store = new ConcurrentHashMap<>();

    @Override
    public void set(Long userId, List<ScoredPost> feedItems) {
        ConcurrentSkipListSet<ScoredPost> sorted = new ConcurrentSkipListSet<>(SCORE_DESC_COMPARATOR);
        if (feedItems != null) {
            sorted.addAll(feedItems);
        }
        store.put(userId, sorted);
    }

    @Override
    public List<ScoredPost> get(Long userId) {
        ConcurrentSkipListSet<ScoredPost> sorted = store.get(userId);
        if (sorted == null) {
            return List.of();
        }
        return new ArrayList<>(sorted);
    }

    @Override
    public void upsert(Long userId, ScoredPost item, int maxSize) {
        ConcurrentSkipListSet<ScoredPost> sorted =
                store.computeIfAbsent(userId, k -> new ConcurrentSkipListSet<>(SCORE_DESC_COMPARATOR));
        sorted.add(item);
        while (sorted.size() > maxSize) {
            sorted.pollLast(); // evict the lowest-scored entry
        }
    }

    @Override
    public void evict(Long userId) {
        store.remove(userId);
    }

    @Override
    public boolean contains(Long userId) {
        ConcurrentSkipListSet<ScoredPost> sorted = store.get(userId);
        return sorted != null && !sorted.isEmpty();
    }
}
