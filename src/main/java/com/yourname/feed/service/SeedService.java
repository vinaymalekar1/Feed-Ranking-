package com.yourname.feed.service;

import com.yourname.feed.dto.SeedResponseDto;
import com.yourname.feed.graph.FollowGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates fake data (users, a random follow graph, and posts with
 * randomized timestamps/likes/comments) so the naive vs ranked feed
 * endpoints can be load-tested. Uses raw JdbcTemplate batch inserts rather
 * than JPA saves, since seeding tens of thousands of rows one entity at a
 * time through Hibernate would be far too slow for this to be a usable
 * dev/test tool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {

    private static final int BATCH_SIZE = 1000;
    private static final String[] CONTENT_TEMPLATES = {
            "Just shipped a new feature, feeling great! #%d",
            "Anyone else think this weather is unreal today? #%d",
            "Hot take: pineapple belongs on pizza. #%d",
            "Reading a great book on system design right now. #%d",
            "Coffee first, code second. #%d",
            "Working on a side project this weekend. #%d",
            "Throwback to last summer's trip. #%d",
            "Excited to announce something big soon... #%d",
            "Debugging is 90% staring, 10% fixing. #%d",
            "Grateful for this community. #%d"
    };

    private final JdbcTemplate jdbcTemplate;
    private final FollowGraph followGraph;

    @Transactional
    public SeedResponseDto seed(int userCount, int postCount) {
        long start = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String runTag = Long.toHexString(System.nanoTime());

        List<Long> userIds = seedUsers(userCount, runTag);
        int followsCreated = seedFollows(userIds, random);
        int postsCreated = seedPosts(userIds, postCount, random);

        long tookMillis = System.currentTimeMillis() - start;
        log.info("Seed complete: {} users, {} follows, {} posts in {} ms",
                userIds.size(), followsCreated, postsCreated, tookMillis);

        return SeedResponseDto.builder()
                .usersCreated(userIds.size())
                .followsCreated(followsCreated)
                .postsCreated(postsCreated)
                .tookMillis(tookMillis)
                .build();
    }

    private List<Long> seedUsers(int userCount, String runTag) {
        Long beforeMaxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM users", Long.class);

        String sql = "INSERT INTO users (username, email, created_at) VALUES (?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < userCount; i++) {
            String username = "user_" + runTag + "_" + i;
            String email = username + "@example.com";
            batchArgs.add(new Object[]{username, email, Timestamp.valueOf(LocalDateTime.now())});

            if (batchArgs.size() == BATCH_SIZE || i == userCount - 1) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        return jdbcTemplate.query(
                "SELECT id FROM users WHERE id > ? ORDER BY id ASC",
                (rs, rowNum) -> rs.getLong("id"),
                beforeMaxId);
    }

    private int seedFollows(List<Long> userIds, ThreadLocalRandom random) {
        if (userIds.size() < 2) {
            return 0;
        }

        String sql = "INSERT INTO follows (follower_id, following_id, created_at) VALUES (?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);
        int totalCreated = 0;
        int n = userIds.size();

        for (int i = 0; i < n; i++) {
            Long followerId = userIds.get(i);
            int followCount = Math.min(n - 1, 5 + random.nextInt(46)); // each user follows 5-50 others
            Set<Integer> chosenIndexes = new HashSet<>();

            int attempts = 0;
            while (chosenIndexes.size() < followCount && attempts < followCount * 10) {
                int candidateIndex = random.nextInt(n);
                if (candidateIndex != i) {
                    chosenIndexes.add(candidateIndex);
                }
                attempts++;
            }

            for (Integer idx : chosenIndexes) {
                Long followingId = userIds.get(idx);
                batchArgs.add(new Object[]{followerId, followingId, Timestamp.valueOf(LocalDateTime.now())});
                followGraph.addFollow(followerId, followingId);
                totalCreated++;

                if (batchArgs.size() == BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batchArgs);
                    batchArgs.clear();
                }
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

        return totalCreated;
    }

    private int seedPosts(List<Long> userIds, int postCount, ThreadLocalRandom random) {
        if (userIds.isEmpty() || postCount <= 0) {
            return 0;
        }

        String sql = "INSERT INTO posts (author_id, content, likes, comments, created_at) VALUES (?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);
        int n = userIds.size();

        for (int i = 0; i < postCount; i++) {
            Long authorId = userIds.get(random.nextInt(n));
            String template = CONTENT_TEMPLATES[random.nextInt(CONTENT_TEMPLATES.length)];
            String content = String.format(template, i);
            int likes = random.nextInt(0, 501);
            int comments = random.nextInt(0, 101);
            // Spread posts across the last 7 days so recency decay is visible.
            long minutesAgo = random.nextLong(0, 7L * 24 * 60);
            Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutesAgo));

            batchArgs.add(new Object[]{authorId, content, likes, comments, createdAt});

            if (batchArgs.size() == BATCH_SIZE || i == postCount - 1) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        return postCount;
    }
}
