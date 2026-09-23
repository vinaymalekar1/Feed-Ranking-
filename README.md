# Feed Ranking System

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-blue?logo=postgresql)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/build-Maven-red?logo=apachemaven)](https://maven.apache.org/)


A Spring Boot backend that demonstrates how social feeds like Twitter/Instagram
rank content — naive (timestamp-only) vs. a ranked feed scored by recency
decay, engagement, and affinity, backed by a hybrid fan-out architecture.

## Features

- **Naive feed** — posts from followed users, sorted purely by `createdAt`.
- **Ranked feed** — top-N posts selected with a `PriorityQueue`-based min-heap
  (`O(M log N)` instead of sorting the whole candidate set), scored by:
  - exponential recency decay (configurable half-life)
  - engagement (`likes * 0.6 + comments * 1.2`)
  - a placeholder affinity signal (swap in real interaction data later)
- **Fan-out-on-write** — new posts from regular users are pushed
  asynchronously (`@Async`) into every follower's precomputed feed.
- **Fan-out-on-read fallback** — authors above a configurable follower
  threshold ("celebrities") skip fan-out-on-write; their posts are pulled
  live at read time instead, avoiding write amplification at scale.
- **In-memory follow graph** — a `Map<Long, Set<Long>>` adjacency list built
  on startup and kept in sync on every new follow, for O(1) lookups.
- **Pluggable cache** — `FeedCache` interface backed today by
  `ConcurrentHashMap` + `ConcurrentSkipListSet`; designed to be swapped for a
  Redis-backed implementation later with no changes to calling code.
- **Load-testing seed endpoint** — generates configurable fake users, a
  random follow graph, and posts via bulk JDBC batch inserts.

## Tech stack

Java 17 · Spring Boot 3 · Spring Data JPA (Hibernate) · PostgreSQL ·
Maven · Lombok · springdoc-openapi (Swagger UI) · JUnit 5

## Project structure

```
com.yourname.feed
├── model/        JPA entities (User, Post, Follow)
├── repository/   Spring Data JPA repositories
├── graph/        FollowGraph adjacency list + startup initializer
├── ranking/      ScoringFunction, ScoredPost, RankingEngine (heap-based top-N)
├── fanout/       FanOutOnWriteService, FanOutOnReadService
├── cache/        FeedCache interface, InMemoryFeedCache impl
├── service/      UserService, PostService, FollowService, FeedService, SeedService
├── controller/   REST controllers
├── config/       AsyncConfig, OpenApiConfig
├── dto/          Request/response DTOs
└── exception/    Global exception handler
```

## Getting started

**Prerequisites:** JDK 17, PostgreSQL, Maven (or use the IDE's bundled Maven).

1. **Create the database**
   ```bash
   psql -U postgres
   CREATE DATABASE feed_ranking_db;
   \q
   ```

2. **Load the schema**
   ```bash
   psql -U postgres -d feed_ranking_db -f src/main/resources/schema.sql
   ```

3. **Configure credentials** — edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/feed_ranking_db
   spring.datasource.username=YOUR_DB_USERNAME
   spring.datasource.password=YOUR_DB_PASSWORD
   ```

4. **Run it**
   ```bash
   mvn spring-boot:run
   ```
   Or run `FeedRankingApplication` directly from your IDE.

5. **Explore the API** — Swagger UI is at
   [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
   A Postman collection is included at
   [`feed-ranking.postman_collection.json`](./feed-ranking.postman_collection.json).

## Try it out

```bash
# Generate fake data
curl -X POST "http://localhost:8080/api/seed?users=5000&posts=50000"

# Naive feed (timestamp only)
curl "http://localhost:8080/api/feed/1/naive"

# Ranked feed (recency decay + engagement + affinity)
curl "http://localhost:8080/api/feed/1"
```

## Configuration

All ranking/fan-out behavior is tunable in `application.properties` under the
`feed.*` prefix — celebrity threshold, cache size per user, ranking weights,
and the async thread pool sizing — with no code changes required.

## Roadmap

- Swap `InMemoryFeedCache` for a Redis-backed `FeedCache` implementation
  (the interface is already shaped for this).
- Replace the placeholder affinity heuristic with real interaction-history
  data (likes/comments/DMs between viewer and author).
