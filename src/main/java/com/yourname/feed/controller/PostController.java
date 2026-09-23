package com.yourname.feed.controller;

import com.yourname.feed.dto.CreatePostRequest;
import com.yourname.feed.dto.PostDto;
import com.yourname.feed.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Basic post CRUD. Creating a post triggers async fan-out-on-write for non-celebrity authors.")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Create a post (triggers async fan-out for non-celebrity authors)")
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a post by id")
    public PostDto getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    @GetMapping
    @Operation(summary = "List all posts")
    public List<PostDto> listPosts() {
        return postService.listPosts();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
