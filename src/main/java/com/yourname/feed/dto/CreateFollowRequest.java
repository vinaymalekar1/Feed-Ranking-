package com.yourname.feed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFollowRequest {

    @NotNull
    private Long followerId;

    @NotNull
    private Long followingId;
}
