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
public class PostDto {
    private Long id;
    private Long authorId;
    private String content;
    private Integer likes;
    private Integer comments;
    private LocalDateTime createdAt;
}
