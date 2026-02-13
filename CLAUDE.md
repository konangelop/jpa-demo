# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./mvnw spring-boot:run          # Start app on localhost:8080
./mvnw test                     # Run all tests
./mvnw test -Dtest=NPlus1DemoTests#entityGraphSolvesNPlus1  # Run single test
./mvnw compile                  # Compile only
```

The app uses H2 in-memory database — no external DB setup needed. H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`).

## Architecture

This is a **teaching/demo project** — not a production app. It demonstrates the JPA N+1 query problem and EntityGraph solutions through side-by-side comparisons with measured query counts.

### Flow: Controller → Service → Repository

- **DemoController** exposes `/api/demo/compare/*` endpoints that return N+1 problem vs EntityGraph solution pairs
- **DemoService** orchestrates each scenario: clears query counter, runs the repository call, maps entities to DTOs, returns `QueryStatsDto` with query count and timing
- **Repositories** define two flavors of each query: a plain `findAll()` (triggers N+1) and an `@EntityGraph`/`JOIN FETCH` variant (solves it)
- **QueryCounter** wraps Hibernate's `Statistics` to measure `prepareStatementCount` between `clear()` and read

### Key design pattern

Every demo scenario follows the same structure in DemoService:
1. `queryCounter.clear()`
2. Call repository method
3. Force-access lazy associations (to trigger or prove absence of N+1)
4. Map to DTOs
5. Return `QueryStatsDto(scenario, description, queryCount, elapsed, results)`

### Entity relationships

All five JPA relationship types are covered with best-practice implementations:

| Entity | Relationship | Pattern |
|--------|-------------|---------|
| Department ↔ DepartmentDetails | @OneToOne | `@MapsId` (shared PK), LAZY, `optional=false` on non-owning side |
| Department → Course | @OneToMany | `mappedBy`, `CascadeType.ALL`, `orphanRemoval`, bidirectional sync helpers |
| Course → Department | @ManyToOne | `FetchType.LAZY` (overrides EAGER default), owns FK |
| Course ↔ Student | @ManyToMany | `Set` (not List), `@JoinTable` on owning side, no `CascadeType.REMOVE` |
| Student ↔ StudentProfile | @OneToOne | `@MapsId` (shared PK) |

All entities use **business-key equals/hashCode** (not generated ID).

### DTOs

All DTOs are Java records in a single file: `dto/DtoClasses.java`. The wrapper `QueryStatsDto` is the standard API response envelope.

### Test data

`src/main/resources/data.sql` seeds 5 departments, 16 courses, 8 students, 20 reviews. Tests assert against these exact counts.

### Test approach

Tests in `NPlus1DemoTests` are `@SpringBootTest @Transactional` integration tests. Each test clears stats, runs a query, forces lazy loading, then asserts query count (N+1 tests assert `> 1`, EntityGraph tests assert `== 1`).

## Conventions

- Entities are plain JPA (no Lombok on entities despite the dependency being present)
- `@ManyToOne` always has `fetch = FetchType.LAZY`
- Repository methods follow naming: `findAllWith*` for EntityGraph variants, `findAllWith*JoinFetch` for JPQL variants
- Collections initialized inline (`new ArrayList<>()`, `new HashSet<>()`)
- Bidirectional relationships have `add*`/`remove*` sync helpers on the parent entity
