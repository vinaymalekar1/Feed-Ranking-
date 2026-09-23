package com.yourname.feed.controller;

import com.yourname.feed.dto.CreateFollowRequest;
import com.yourname.feed.dto.FollowDto;
import com.yourname.feed.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@Tag(name = "Follows", description = "Follow graph CRUD, kept in sync with the in-memory FollowGraph")
public class FollowController {

    private final FollowService followService;

    @PostMapping
    @Operation(summary = "Follow a user")
    public ResponseEntity<FollowDto> follow(@Valid @RequestBody CreateFollowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followService.follow(request));
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "List who this user follows")
    public List<FollowDto> getFollowing(@PathVariable Long userId) {
        return followService.getFollowing(userId);
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "List who follows this user")
    public List<FollowDto> getFollowers(@PathVariable Long userId) {
        return followService.getFollowers(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Unfollow (delete a follow relationship by its id)")
    public ResponseEntity<Void> unfollow(@PathVariable Long id) {
        followService.unfollow(id);
        return ResponseEntity.noContent().build();
    }
}
