package com.yourname.feed.ranking;

import com.yourname.feed.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A Post paired with its computed ranking score at the time of scoring.
 * Kept intentionally simple/serializable so it can live in the FeedCache
 * and later be swapped into a Redis-backed cache with minimal changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoredPost implements Serializable {
    private Post post;
    private double score;
}
