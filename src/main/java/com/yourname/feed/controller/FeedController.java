package com.yourname.feed.controller;

import com.yourname.feed.dto.FeedResponseDto;
import com.yourname.feed.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "Naive (timestamp-only) vs ranked (scored) feed endpoints")
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/{userId}/naive")
    @Operation(summary = "Naive feed: posts from followed users sorted by timestamp only")
    public FeedResponseDto getNaiveFeed(
            @PathVariable Long userId,
            @Parameter(description = "Max number of posts to return") @RequestParam(defaultValue = "20") int limit) {
        return feedService.getNaiveFeed(userId, limit);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Ranked feed: top posts by recency-decayed engagement + affinity score, "
            + "merging the fan-out-on-write cache with live fan-out-on-read results for celebrity authors")
    public FeedResponseDto getRankedFeed(
            @PathVariable Long userId,
            @Parameter(description = "Max number of posts to return") @RequestParam(defaultValue = "20") int limit) {
        return feedService.getRankedFeed(userId, limit);
    }
}
