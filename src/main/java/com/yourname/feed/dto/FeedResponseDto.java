package com.yourname.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedResponseDto {
    private Long userId;
    private String feedType; // "naive" or "ranked"
    private int count;
    private List<FeedItemDto> items;
}
