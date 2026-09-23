package com.yourname.feed.service;

import com.yourname.feed.dto.CreatePostRequest;
import com.yourname.feed.dto.PostDto;
import com.yourname.feed.fanout.FanOutOnWriteService;
import com.yourname.feed.graph.FollowGraph;
import com.yourname.feed.model.Post;
import com.yourname.feed.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FollowGraph followGraph;
    private final FanOutOnWriteService fanOutOnWriteService;

    @Value("${feed.celebrity-threshold:10000}")
    private int celebrityThreshold;

    public PostDto createPost(CreatePostRequest request) {
        Post post = Post.builder()
                .authorId(request.getAuthorId())
                .content(request.getContent())
                .likes(0)
                .comments(0)
                .build();

        Post saved = postRepository.save(post);

        // Celebrity authors skip fan-out-on-write; their posts are pulled
        // live at read time instead (see FanOutOnReadService).
        if (!followGraph.isCelebrity(saved.getAuthorId(), celebrityThreshold)) {
            fanOutOnWriteService.fanOut(saved);
        }

        return toDto(saved);
    }

    public PostDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));
        return toDto(post);
    }

    public List<PostDto> listPosts() {
        return postRepository.findAll().stream().map(this::toDto).toList();
    }

    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new EntityNotFoundException("Post not found: " + id);
        }
        postRepository.deleteById(id);
    }

    private PostDto toDto(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .likes(post.getLikes())
                .comments(post.getComments())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
