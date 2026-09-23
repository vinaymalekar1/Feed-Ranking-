package com.yourname.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowDto {
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}
