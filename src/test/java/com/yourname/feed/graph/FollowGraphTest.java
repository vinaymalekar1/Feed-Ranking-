package com.yourname.feed.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FollowGraphTest {

    private FollowGraph followGraph;

    @BeforeEach
    void setUp() {
        followGraph = new FollowGraph();
    }

    @Test
    void addFollowUpdatesBothDirections() {
        followGraph.addFollow(1L, 2L);

        assertTrue(followGraph.getFollowing(1L).contains(2L));
        assertTrue(followGraph.getFollowers(2L).contains(1L));
        assertTrue(followGraph.isFollowing(1L, 2L));
        assertFalse(followGraph.isFollowing(2L, 1L));
    }

    @Test
    void removeFollowUpdatesBothDirections() {
        followGraph.addFollow(1L, 2L);
        followGraph.removeFollow(1L, 2L);

        assertFalse(followGraph.getFollowing(1L).contains(2L));
        assertFalse(followGraph.getFollowers(2L).contains(1L));
    }

    @Test
    void unknownUserReturnsEmptySets() {
        assertTrue(followGraph.getFollowing(999L).isEmpty());
        assertTrue(followGraph.getFollowers(999L).isEmpty());
        assertEquals(0, followGraph.getFollowerCount(999L));
    }

    @Test
    void isCelebrityRespectsThreshold() {
        for (long i = 1; i <= 15; i++) {
            followGraph.addFollow(i, 100L);
        }
        assertEquals(15, followGraph.getFollowerCount(100L));
        assertTrue(followGraph.isCelebrity(100L, 10));
        assertFalse(followGraph.isCelebrity(100L, 20));
    }
}
