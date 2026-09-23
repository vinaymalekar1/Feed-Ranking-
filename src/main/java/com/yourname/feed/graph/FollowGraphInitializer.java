package com.yourname.feed.graph;

import com.yourname.feed.model.Follow;
import com.yourname.feed.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads the full follow graph from the database into memory once on startup.
 * Runs after the Spring context is fully initialized (ApplicationRunner),
 * so JPA/DataSource beans are ready.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowGraphInitializer implements ApplicationRunner {

    private final FollowRepository followRepository;
    private final FollowGraph followGraph;

    @Override
    public void run(ApplicationArguments args) {
        followGraph.clear();
        List<Follow> allFollows = followRepository.findAll();
        for (Follow follow : allFollows) {
            followGraph.addFollow(follow.getFollowerId(), follow.getFollowingId());
        }
        log.info("FollowGraph initialized: {} edges loaded, {} users with at least one 'following' entry",
                allFollows.size(), followGraph.totalUsers());
    }
}
