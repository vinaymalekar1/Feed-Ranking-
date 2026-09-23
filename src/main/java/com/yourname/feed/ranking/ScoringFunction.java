package com.yourname.feed.ranking;

import com.yourname.feed.model.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Computes a single ranking score for a post, combining:
 *   1. Recency decay      - exponential decay based on hours since posted
 *   2. Engagement score   - likes * likeWeight + comments * commentWeight
 *   3. Affinity score     - placeholder "how much does the viewer care about
 *                           this author" signal (see {@link #placeholderAffinity})
 *
 * All weights are configurable via application.yml (feed.ranking.*) so they
 * can be tuned without a code change.
 */
@Component
public class ScoringFunction {

    private final double decayLambda;
    private final double likeWeight;
    private final double commentWeight;
    private final double affinityWeight;

    public ScoringFunction(
            @Value("${feed.ranking.decay-lambda:0.08}") double decayLambda,
            @Value("${feed.ranking.like-weight:0.6}") double likeWeight,
            @Value("${feed.ranking.comment-weight:1.2}") double commentWeight,
            @Value("${feed.ranking.affinity-weight:5.0}") double affinityWeight
    ) {
        this.decayLambda = decayLambda;
        this.likeWeight = likeWeight;
        this.commentWeight = commentWeight;
        this.affinityWeight = affinityWeight;
    }

    /**
     * Final ranking score for a post, given a precomputed affinity value
     * (see {@link #placeholderAffinity}).
     */
    public double score(Post post, double affinity) {
        double hoursSincePosted = hoursSince(post.getCreatedAt());
        double recencyDecay = Math.exp(-decayLambda * hoursSincePosted);

        double engagementScore = post.getLikes() * likeWeight + post.getComments() * commentWeight;
        double affinityScore = affinity * affinityWeight;

        // Both engagement and affinity are recency-weighted, so a two-year-old
        // viral post doesn't permanently outrank everything new.
        return (engagementScore + affinityScore) * recencyDecay;
    }

    private double hoursSince(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0.0;
        }
        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        return Math.max(0.0, minutes / 60.0);
    }

    /**
     * PLACEHOLDER affinity heuristic.
     *
     * A real implementation would derive this from historical interaction
     * data (likes/comments/DMs/profile visits between viewer and author,
     * shared interests, mutual follows, etc.), typically stored in a
     * separate interactions table or feature store. For this demo we don't
     * have that data model, so we return a small deterministic value derived
     * from the author id purely so the ranked feed has *some* affinity
     * signal beyond engagement + recency. Replace this method first when
     * wiring up real personalization.
     */
    public double placeholderAffinity(Long viewerId, Long authorId) {
        if (authorId == null) {
            return 1.0;
        }
        return 1.0 + (Math.abs(authorId % 5) * 0.1);
    }
}
