# JPA N+1 Problem & EntityGraph Demo

A comprehensive Spring Boot project that demonstrates the **N+1 query problem** in JPA/Hibernate, how **EntityGraph** solves it, and the **most efficient implementation patterns** for all JPA relationship types.

## Table of Contents

- [Quick Start](#quick-start)
- [Domain Model](#domain-model)
- [The N+1 Problem Explained](#the-n1-problem-explained)
- [EntityGraph: The Solution](#entitygraph-the-solution)
- [FETCH vs LOAD EntityGraph](#fetch-vs-load-entitygraph)
- [Named EntityGraph (Alternative)](#named-entitygraph-alternative)
- [JPA Relationship Best Practices](#jpa-relationship-best-practices)
- [API Endpoints](#api-endpoints)
- [Running the Tests](#running-the-tests)
- [Other N+1 Solutions](#other-n1-solutions)

---

## Quick Start

```bash
# Clone and run
./mvnw spring-boot:run

# Open browser
http://localhost:8080/api/demo/all           # Run ALL scenarios
http://localhost:8080/api/demo/compare/departments  # Side-by-side comparison
http://localhost:8080/h2-console              # Database console (JDBC URL: jdbc:h2:mem:testdb)
```

The API responses include **query counts** so you can see the difference between N+1 and optimized queries directly in the JSON output. SQL logs are also printed to the console.

---

## Domain Model

```
┌─────────────────┐       ┌─────────────────────┐
│   Department    │1    1 │  DepartmentDetails   │
│─────────────────│───────│─────────────────────│
│ id              │       │ department_id (PK/FK)│  ← @OneToOne with @MapsId
│ name            │       │ building             │    (shared primary key)
│ description     │       │ budget               │
└────────┬────────┘       │ headOfDepartment     │
         │                └─────────────────────┘
         │ 1
         │
         │ *
┌────────┴────────┐       ┌──────────────────┐
│     Course      │*    * │     Student      │
│─────────────────│───────│──────────────────│
│ id              │       │ id               │
│ title           │  via  │ firstName        │   ← @ManyToMany
│ description     │ join  │ lastName         │     (Set, not List!)
│ credits         │ table │ email            │
│ department_id FK│       └────────┬─────────┘
└────────┬────────┘                │ 1
         │                         │
         │ 1                       │ 1
         │                ┌────────┴─────────┐
         │ *              │  StudentProfile   │
┌────────┴────────┐       │──────────────────│
│     Review      │       │ student_id (PK/FK)│  ← @OneToOne with @MapsId
│─────────────────│       │ bio              │
│ id              │       │ dateOfBirth      │
│ content         │       │ phoneNumber      │
│ rating          │       │ address          │
│ course_id FK    │       └──────────────────┘
│ student_id FK   │
└─────────────────┘
```

**Relationships covered:**

| Type | Example | FK Owner |
|------|---------|----------|
| `@OneToOne` (shared PK) | Department ↔ DepartmentDetails | DepartmentDetails (via `@MapsId`) |
| `@OneToOne` (shared PK) | Student ↔ StudentProfile | StudentProfile (via `@MapsId`) |
| `@OneToMany` / `@ManyToOne` | Department → Course | Course (`department_id`) |
| `@OneToMany` / `@ManyToOne` | Course → Review | Review (`course_id`) |
| `@ManyToOne` | Review → Student | Review (`student_id`) |
| `@ManyToMany` | Course ↔ Student | Course (owns `@JoinTable`) |

---

## The N+1 Problem Explained

### What is it?

The N+1 problem occurs when loading a list of entities triggers **one additional query per entity** to fetch a lazy association. If you load N departments and access each department's courses, you get:

```
Query 1:   SELECT * FROM departments                          -- Loads N departments
Query 2:   SELECT * FROM courses WHERE department_id = 1      -- Dept 1's courses
Query 3:   SELECT * FROM courses WHERE department_id = 2      -- Dept 2's courses
Query 4:   SELECT * FROM courses WHERE department_id = 3      -- Dept 3's courses
...
Query N+1: SELECT * FROM courses WHERE department_id = N      -- Dept N's courses
```

**Total: N+1 queries.** With 1000 departments, that's 1001 queries!

### Why does it happen?

JPA defaults to **LAZY loading** for collections (`@OneToMany`, `@ManyToMany`). When you call `findAll()`, only the parent entities are loaded. The collections are replaced by **Hibernate proxies** that execute a SELECT when first accessed.

The problem also occurs with `@ManyToOne` and `@OneToOne` when accessed. Although `@ManyToOne` defaults to `EAGER` (which causes a different performance issue), best practice is to set it to `LAZY`, and then the N+1 manifests when you access the association.

### Where to see it in this project

```java
// In DemoService.java — THE PROBLEM
List<Department> departments = departmentRepository.findAll();  // 1 query

departments.forEach(d -> {
    d.getCourses().size();  // N queries! One SELECT per department!
});
```

Hit `GET /api/demo/compare/departments` and observe:
- `problem_nplus1.queryCount` — Shows the inflated query count
- `solution_entitygraph.queryCount` — Shows it solved with 1 query

---

## EntityGraph: The Solution

### What is EntityGraph?

`EntityGraph` is a JPA 2.1 feature that tells the persistence provider: **"When loading this entity, also fetch these associations in the SAME query using JOINs."**

Instead of N+1 separate queries, you get **1 query** with LEFT JOINs:

```sql
-- Without EntityGraph (N+1):
SELECT * FROM departments;
SELECT * FROM courses WHERE department_id = 1;
SELECT * FROM courses WHERE department_id = 2;
-- ... N more queries

-- With EntityGraph (1 query):
SELECT d.*, c.*
FROM departments d
LEFT JOIN courses c ON d.id = c.department_id;
```

### Three ways to define EntityGraph

#### 1. `@EntityGraph` annotation on repository methods (Recommended)

The simplest approach. Works with both derived query methods and `@Query` methods:

```java
// On a derived query method
@EntityGraph(attributePaths = {"courses"})
List<Department> findAllWithCourses();

// On a JPQL query
@EntityGraph(attributePaths = {"courses", "details"})
@Query("SELECT d FROM Department d WHERE d.name LIKE %:name%")
List<Department> findByNameContainingWithCourses(String name);
```

#### 2. `JOIN FETCH` in JPQL (Alternative)

More explicit and verbose, but gives you full control:

```java
@Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.courses")
List<Department> findAllWithCoursesJoinFetch();
```

**Note:** `DISTINCT` is required because the JOIN produces duplicate parent rows (one per child). Without it, if a department has 3 courses, that department appears 3 times in the result.

#### 3. `@NamedEntityGraph` on the entity class

Reusable graph definitions on the entity (see [Named EntityGraph section](#named-entitygraph-alternative)).

### Nested EntityGraph (Multi-level fetch)

You can fetch associations multiple levels deep using dot notation:

```java
// Department → courses → students (2 levels deep)
@EntityGraph(attributePaths = {"courses", "courses.students"})
@Query("SELECT DISTINCT d FROM Department d")
List<Department> findAllWithCoursesAndStudents();

// Department → courses → reviews → student (3 levels deep)
@EntityGraph(attributePaths = {"courses", "courses.reviews", "courses.reviews.student"})
@Query("SELECT DISTINCT d FROM Department d WHERE d.id = :id")
Optional<Department> findByIdWithFullGraph(Long id);
```

**⚠️ Warning: Cartesian Product**

Fetching multiple collections at the same level (e.g., `courses` AND `reviews` from the same entity) creates a Cartesian product. If a course has 10 reviews and 5 students, the result set has 50 rows per course. For large datasets, consider:
- Fetching only one collection per query
- Using `@BatchSize` for secondary collections
- Using separate queries combined in the service layer

---

## FETCH vs LOAD EntityGraph

EntityGraph has two types that differ in how they handle attributes NOT listed in the graph:

### FETCH Graph (Default)

```java
@EntityGraph(attributePaths = {"courses"}, type = EntityGraphType.FETCH)
```

- Attributes **IN** the graph → Loaded **EAGERLY** (via JOIN)
- Attributes **NOT** in the graph → Loaded **LAZILY** (regardless of entity annotation)

Use when you want **strict control** — only what you specify is fetched.

### LOAD Graph

```java
@EntityGraph(attributePaths = {"courses"}, type = EntityGraphType.LOAD)
```

- Attributes **IN** the graph → Loaded **EAGERLY** (via JOIN)
- Attributes **NOT** in the graph → Use their **entity-defined** fetch type

Use when you want to **add** extra fetching on top of existing configuration.

### Example comparison

Given `Department` with `courses` (LAZY) and `details` (LAZY):

| Graph Type | `courses` | `details` |
|-----------|-----------|-----------|
| **FETCH** `{"courses"}` | EAGER ✅ | LAZY (overrides entity) |
| **LOAD** `{"courses"}` | EAGER ✅ | LAZY (uses entity default) |

In this case they behave the same because `details` is LAZY on the entity. The difference appears if `details` were EAGER on the entity: FETCH would make it LAZY, LOAD would keep it EAGER.

Hit `GET /api/demo/compare/fetch-vs-load` to see this in action.

---

## Named EntityGraph (Alternative)

Instead of specifying `attributePaths` on every repository method, you can define reusable graphs on the entity:

```java
@Entity
@NamedEntityGraph(
    name = "Course.withDepartment",
    attributeNodes = @NamedAttributeNode("department")
)
@NamedEntityGraph(
    name = "Course.withAll",
    attributeNodes = {
        @NamedAttributeNode("department"),
        @NamedAttributeNode(value = "reviews"),
        @NamedAttributeNode(value = "students")
    }
)
public class Course { ... }
```

Then reference by name in the repository:

```java
@EntityGraph(value = "Course.withDepartment")
List<Course> findAll();
```

**When to use each approach:**

| Approach | Pros | Cons |
|----------|------|------|
| `attributePaths` | Simple, flexible, no entity changes | Duplicated if used in many methods |
| `@NamedEntityGraph` | Reusable, single source of truth | Clutters entity, less flexible |
| `JOIN FETCH` in JPQL | Full control, no annotations | Verbose, must remember DISTINCT |

This project uses `attributePaths` throughout for clarity, but all three are valid.

---

## JPA Relationship Best Practices

### @OneToOne — Use `@MapsId` (Shared Primary Key)

**Bad approach (extra column):**
```java
@Entity
public class DepartmentDetails {
    @Id @GeneratedValue
    private Long id;              // Separate PK

    @OneToOne
    @JoinColumn(name = "department_id", unique = true)
    private Department department; // FK with UNIQUE constraint
}
// Result: department_details(id PK, department_id FK UNIQUE) — wasteful extra column
```

**Good approach (shared PK with `@MapsId`):**
```java
@Entity
public class DepartmentDetails {
    @Id
    private Long id;               // PK is SAME as department's PK

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId                         // PK derived from the association
    @JoinColumn(name = "department_id")
    private Department department;
}
// Result: department_details(department_id PK/FK) — clean and efficient
```

**Why `@MapsId` is better:**
- Eliminates redundant columns (PK is the FK)
- No extra unique constraint needed
- Cleaner database schema
- Makes the 1:1 relationship unambiguous

**⚠️ Lazy loading gotcha on `@OneToOne`:**
On the **non-owning side** (the side with `mappedBy`), LAZY loading only works if:
- `optional = false` (the relationship is guaranteed to exist), OR
- Bytecode enhancement is enabled

Without these, Hibernate must issue a query just to check if the related entity exists, defeating LAZY. Using `@MapsId` on the owning side is the recommended workaround.

### @OneToMany / @ManyToOne — Always use `mappedBy`

**Bad approach (generates join table):**
```java
@OneToMany
private List<Course> courses;
// Result: Creates a departments_courses join table. Extra INSERTs/DELETEs.
```

**Good approach (FK on child side):**
```java
// Parent side (Department)
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Course> courses = new ArrayList<>();

// Child side (Course) — OWNS the FK
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "department_id", nullable = false)
private Department department;
```

**Key rules:**
1. **Always set `fetch = FetchType.LAZY` on `@ManyToOne`** — it defaults to EAGER!
2. **Use `mappedBy`** on the `@OneToMany` side — otherwise JPA creates a join table
3. **Initialize collections** to empty `ArrayList` — avoids `NullPointerException`
4. **Use bidirectional sync helpers** — keep both sides in sync:

```java
public void addCourse(Course course) {
    courses.add(course);
    course.setDepartment(this);
}

public void removeCourse(Course course) {
    courses.remove(course);
    course.setDepartment(null);
}
```

### @ManyToMany — Use `Set`, not `List`

**Bad approach (List = bag semantics):**
```java
@ManyToMany
@JoinTable(name = "course_students", ...)
private List<Student> students = new ArrayList<>();
```

When removing a student, Hibernate:
1. **DELETEs ALL rows** from `course_students` for this course
2. **Re-INSERTs** all remaining associations

This is because `List` is treated as a "bag" (unordered, allows duplicates) and Hibernate can't efficiently remove a single row.

**Good approach (Set = efficient operations):**
```java
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(
    name = "course_students",
    joinColumns = @JoinColumn(name = "course_id"),
    inverseJoinColumns = @JoinColumn(name = "student_id")
)
private Set<Student> students = new HashSet<>();
```

With `Set`, Hibernate generates a targeted `DELETE` for just the removed row.

**Additional `@ManyToMany` rules:**
1. **Never use `CascadeType.REMOVE` or `ALL`** — removing a student should not delete their courses
2. **Only the owning side defines `@JoinTable`** — the inverse side uses `mappedBy`
3. **Implement `equals/hashCode`** on business keys for `Set` correctness
4. **For complex relationships**, consider an explicit join entity (e.g., `Enrollment`) with `@ManyToOne` on each side — allows extra columns like `enrollmentDate`

### equals/hashCode — Use Business Keys

**Bad: Using generated `@Id`**
```java
@Override
public boolean equals(Object o) {
    Student that = (Student) o;
    return Objects.equals(id, that.id);  // NULL before persist!
}
```

Before `persist()`, `id` is null for all new entities, so they all appear "equal" in a `Set`.

**Good: Using natural/business key**
```java
@Override
public boolean equals(Object o) {
    Student that = (Student) o;
    return Objects.equals(email, that.email);  // Unique and stable
}
```

Use a column that is `unique`, `not null`, and **immutable** (like email, ISBN, SSN).

---

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/demo/compare/departments` | OneToMany: N+1 vs EntityGraph vs JOIN FETCH |
| `GET /api/demo/compare/courses` | ManyToOne: N+1 vs EntityGraph |
| `GET /api/demo/compare/reviews` | Dual ManyToOne: N+1 vs EntityGraph |
| `GET /api/demo/compare/students` | ManyToMany: N+1 vs EntityGraph |
| `GET /api/demo/compare/departments-full` | Multiple associations: double N+1 vs multi-path EntityGraph |
| `GET /api/demo/compare/fetch-vs-load` | FETCH graph vs LOAD graph behavior |
| `GET /api/demo/nested-entitygraph` | Nested graph: Department → Courses → Students |
| `GET /api/demo/all` | Run ALL scenarios at once |
| `GET /h2-console` | H2 Database web console |

Each response includes:
- `scenario` — Description of what's being tested
- `queryCount` — Number of SQL statements executed
- `executionTimeMs` — Wall clock time
- `results` — The actual data

---

## Running the Tests

```bash
./mvnw test
```

The tests verify that:
- N+1 queries occur without EntityGraph (query count > 1)
- EntityGraph reduces to exactly 1 query
- JOIN FETCH achieves the same as EntityGraph
- Nested EntityGraph works for multi-level fetch
- All relationship types function correctly

---

## Other N+1 Solutions

While EntityGraph is the primary focus of this project, there are other approaches:

### `@BatchSize` (Hibernate-specific)

```java
@OneToMany(mappedBy = "department")
@BatchSize(size = 16)
private List<Course> courses;
```

Instead of N individual queries, Hibernate batches them using `IN` clauses:
```sql
SELECT * FROM courses WHERE department_id IN (1, 2, 3, ..., 16)
```

This reduces N+1 to `⌈N/batchSize⌉ + 1` queries. Set globally in `application.yml`:
```yaml
spring.jpa.properties.hibernate.default_batch_fetch_size: 16
```

**When to use:** As a safety net alongside EntityGraph. Good default behavior, but EntityGraph is still more optimal (1 query vs few).

### DTO Projections with JPQL

```java
@Query("SELECT new com.example.dto.CourseSummary(c.title, d.name) " +
       "FROM Course c JOIN c.department d")
List<CourseSummary> findCourseSummaries();
```

**When to use:** When you only need specific columns, not full entities. Most efficient for read-only views.

### Subselect Fetching (Hibernate-specific)

```java
@OneToMany(mappedBy = "department")
@Fetch(FetchMode.SUBSELECT)
private List<Course> courses;
```

Uses a subquery to batch-fetch all collections at once:
```sql
SELECT * FROM courses WHERE department_id IN (SELECT id FROM departments)
```

**When to use:** When all parents' collections will be accessed and you want automatic optimization.

---

## Summary Cheat Sheet

| Problem | Solution | Queries |
|---------|----------|---------|
| N+1 on `@OneToMany` | `@EntityGraph(attributePaths = {"collection"})` | 1 |
| N+1 on `@ManyToOne` | `@EntityGraph(attributePaths = {"association"})` | 1 |
| N+1 on `@ManyToMany` | `@EntityGraph(attributePaths = {"collection"})` | 1 |
| N+1 on multiple assocs | `@EntityGraph(attributePaths = {"a", "b"})` | 1 |
| N+1 nested | `@EntityGraph(attributePaths = {"a", "a.b"})` | 1 |
| Global safety net | `hibernate.default_batch_fetch_size: 16` | ⌈N/16⌉+1 |
| Read-only views | DTO projection with `new` in JPQL | 1 |

**Golden rules:**
1. **Always set `@ManyToOne(fetch = FetchType.LAZY)`**
2. **Use `@EntityGraph` or `JOIN FETCH` when you know you'll access associations**
3. **Use `Set` for `@ManyToMany`, not `List`**
4. **Use `@MapsId` for `@OneToOne`**
5. **Use `mappedBy` on `@OneToMany`**
6. **Use bidirectional sync helpers**
7. **Use DTOs for API responses**
8. **Enable `hibernate.generate_statistics` during development to catch N+1 early**
