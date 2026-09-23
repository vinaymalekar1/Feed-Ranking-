package com.yourname.feed.service;

import com.yourname.feed.dto.CreateFollowRequest;
import com.yourname.feed.dto.FollowDto;
import com.yourname.feed.graph.FollowGraph;
import com.yourname.feed.model.Follow;
import com.yourname.feed.repository.FollowRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowGraph followGraph;

    public FollowDto follow(CreateFollowRequest request) {
        if (request.getFollowerId().equals(request.getFollowingId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A user cannot follow themselves");
        }
        if (followRepository.existsByFollowerIdAndFollowingId(request.getFollowerId(), request.getFollowingId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following");
        }

        Follow follow = Follow.builder()
                .followerId(request.getFollowerId())
                .followingId(request.getFollowingId())
                .build();

        Follow saved = followRepository.save(follow);

        // Keep the in-memory graph in sync immediately, so ranking/fan-out
        // logic sees this edge without waiting for a restart.
        followGraph.addFollow(saved.getFollowerId(), saved.getFollowingId());

        return toDto(saved);
    }

    public List<FollowDto> getFollowing(Long userId) {
        return followRepository.findByFollowerId(userId).stream().map(this::toDto).toList();
    }

    public List<FollowDto> getFollowers(Long userId) {
        return followRepository.findByFollowingId(userId).stream().map(this::toDto).toList();
    }

    public void unfollow(Long id) {
        Follow follow = followRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Follow relationship not found: " + id));
        followRepository.deleteById(id);
        followGraph.removeFollow(follow.getFollowerId(), follow.getFollowingId());
    }

    private FollowDto toDto(Follow follow) {
        return FollowDto.builder()
                .id(follow.getId())
                .followerId(follow.getFollowerId())
                .followingId(follow.getFollowingId())
                .createdAt(follow.getCreatedAt())
                .build();
    }
}
