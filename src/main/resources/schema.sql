-- ============================================================================
-- feed-ranking : PostgreSQL schema
-- Run this manually against your local database before starting the app
-- (see setup instructions). Hibernate is configured with ddl-auto: validate,
-- so it will NOT create or alter tables itself.
-- ============================================================================

DROP TABLE IF EXISTS follows CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ----------------------------------------------------------------------------
-- users
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ----------------------------------------------------------------------------
-- posts
-- ----------------------------------------------------------------------------
CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    author_id   BIGINT NOT NULL,
    content     TEXT NOT NULL,
    likes       INTEGER NOT NULL DEFAULT 0,
    comments    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Critical for both the naive feed query (posts from a set of authors, newest
-- first) and the fan-out-on-read live pull for celebrity authors.
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC);
CREATE INDEX idx_posts_created ON posts (created_at DESC);

-- ----------------------------------------------------------------------------
-- follows
-- ----------------------------------------------------------------------------
CREATE TABLE follows (
    id            BIGSERIAL PRIMARY KEY,
    follower_id   BIGINT NOT NULL,
    following_id  BIGINT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_follows_pair UNIQUE (follower_id, following_id),
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> following_id)
);

-- Used to build the FollowGraph adjacency list on startup and for
-- "who does X follow" / "who follows X" (celebrity threshold) lookups.
CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_following_id ON follows (following_id);
