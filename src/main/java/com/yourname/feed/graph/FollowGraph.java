package com.yourname.feed.graph;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adjacency list representation of the follow graph, built from the
 * `follows` table on startup (see {@link FollowGraphInitializer}) and kept in
 * sync whenever a new follow is created.
 *
 * Two maps are kept so both directions are O(1) lookups:
 *  - following: userId -> set of userIds they follow
 *  - followers: userId -> set of userIds that follow them (used to cheaply
 *    check "does this author exceed the celebrity threshold?")
 */
@Component
public class FollowGraph {

    private final Map<Long, Set<Long>> following = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> followers = new ConcurrentHashMap<>();

    public void addFollow(Long followerId, Long followingId) {
        following.computeIfAbsent(followerId, k -> ConcurrentHashMap.newKeySet()).add(followingId);
        followers.computeIfAbsent(followingId, k -> ConcurrentHashMap.newKeySet()).add(followerId);
    }

    public void removeFollow(Long followerId, Long followingId) {
        Set<Long> f = following.get(followerId);
        if (f != null) {
            f.remove(followingId);
        }
        Set<Long> b = followers.get(followingId);
        if (b != null) {
            b.remove(followerId);
        }
    }

    /** Who does this user follow? */
    public Set<Long> getFollowing(Long userId) {
        return following.getOrDefault(userId, Set.of());
    }

    /** Who follows this user? */
    public Set<Long> getFollowers(Long userId) {
        return followers.getOrDefault(userId, Set.of());
    }

    public int getFollowerCount(Long userId) {
        return getFollowers(userId).size();
    }

    /** Does this user's follower count exceed the celebrity fan-out threshold? */
    public boolean isCelebrity(Long userId, int celebrityThreshold) {
        return getFollowerCount(userId) > celebrityThreshold;
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return getFollowing(followerId).contains(followingId);
    }

    public void clear() {
        following.clear();
        followers.clear();
    }

    public int totalUsers() {
        return following.size();
    }
}
