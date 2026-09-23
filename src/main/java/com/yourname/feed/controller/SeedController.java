package com.yourname.feed.controller;

import com.yourname.feed.dto.SeedResponseDto;
import com.yourname.feed.service.SeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
@Tag(name = "Seed", description = "Generate fake data for load-testing naive vs ranked feed performance")
public class SeedController {

    private final SeedService seedService;

    @PostMapping
    @Operation(summary = "Generate N users with a random follow graph and M posts with randomized data")
    public SeedResponseDto seed(
            @Parameter(description = "Number of users to create") @RequestParam(defaultValue = "5000") int users,
            @Parameter(description = "Number of posts to create") @RequestParam(defaultValue = "50000") int posts) {
        return seedService.seed(users, posts);
    }
}
