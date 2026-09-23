package com.yourname.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeedResponseDto {
    private int usersCreated;
    private int followsCreated;
    private int postsCreated;
    private long tookMillis;
}
