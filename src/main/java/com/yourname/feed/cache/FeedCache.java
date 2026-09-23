package com.yourname.feed.cache;

import com.yourname.feed.ranking.ScoredPost;

import java.util.List;

/**
 * Abstraction over the precomputed-feed cache used by the fan-out-on-write
 * path. The only implementation today is {@link InMemoryFeedCache}
 * (ConcurrentHashMap + ConcurrentSkipListSet), but this interface is
 * intentionally shaped so a future RedisFeedCache implementation can be
 * dropped in with no changes required in FanOutOnWriteService/FeedService -
 * just swap the @Component that gets wired.
 */
public interface FeedCache {

    /** Replace this user's entire precomputed feed. */
    void set(Long userId, List<ScoredPost> feedItems);

    /** Read this user's precomputed feed (highest score first). Empty list if none cached. */
    List<ScoredPost> get(Long userId);

    /**
     * Insert a single scored post into this user's precomputed feed
     * (used by fan-out-on-write when a followed user creates a new post),
     * trimming the feed down to maxSize afterward, keeping the highest-scored
     * entries.
     */
    void upsert(Long userId, ScoredPost item, int maxSize);

    void evict(Long userId);

    boolean contains(Long userId);
}
