package com.example.jpanplus1.repository;

import com.example.jpanplus1.entity.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository demonstrating ALL EntityGraph approaches for solving N+1.
 *
 * EntityGraph tells JPA: "When you load this entity, also fetch these associations
 * in the SAME query using a JOIN." This replaces N+1 separate queries with 1 query.
 *
 * There are TWO types of EntityGraph:
 *
 * 1. FETCH graph (EntityGraphType.FETCH) — DEFAULT
 *    Attributes in the graph → EAGER
 *    Attributes NOT in the graph → LAZY (regardless of entity annotation)
 *    Use when: You want ONLY the specified associations, nothing else.
 *
 * 2. LOAD graph (EntityGraphType.LOAD)
 *    Attributes in the graph → EAGER
 *    Attributes NOT in the graph → Use their entity-defined fetch type
 *    Use when: You want to ADD extra fetching on top of existing config.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // =========================================================================
    // APPROACH 1: @EntityGraph annotation on Spring Data query methods
    // This is the simplest and most common approach.
    // =========================================================================

    /**
     * Fetches all departments WITH their courses in a SINGLE query.
     * Without this: findAll() → N queries to load courses when accessed.
     *
     * attributePaths: List of association paths to eagerly fetch.
     * type = FETCH (default): Only these paths are eager; everything else is lazy.
     */
    @EntityGraph(attributePaths = {"courses"}, type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT d FROM Department d")
    List<Department> findAllWithCourses();

    /**
     * Fetches departments with BOTH courses AND details in one query.
     * Multiple paths = multiple JOINs in the generated SQL.
     */
    @EntityGraph(attributePaths = {"courses", "details"}, type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT d FROM Department d")
    List<Department> findAllWithCoursesAndDetails();

    /**
     * LOAD graph example: Fetches courses eagerly, but other associations
     * use their entity-defined fetch type (e.g., if details was EAGER on entity,
     * it would still be EAGER here).
     */
    @EntityGraph(attributePaths = {"courses"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT d FROM Department d")
    List<Department> findAllWithCoursesLoadGraph();

    // =========================================================================
    // APPROACH 2: @EntityGraph with JPQL queries
    // Combine EntityGraph with custom JPQL for filtering + eager fetching.
    // =========================================================================

    /**
     * EntityGraph + JPQL: Filter by name AND eagerly fetch courses.
     * The EntityGraph is applied ON TOP of the JPQL query.
     */
    @EntityGraph(attributePaths = {"courses"})
    @Query("SELECT d FROM Department d WHERE d.name LIKE %:name%")
    List<Department> findByNameContainingWithCourses(String name);

    // =========================================================================
    // APPROACH 3: @EntityGraph with nested paths (sub-graphs)
    // Fetch nested associations: Department → courses → students
    // =========================================================================

    /**
     * NESTED EntityGraph: Fetches Department → courses → students in ONE query.
     *
     * attributePaths = {"courses.students"} tells JPA:
     *   1. JOIN courses (first level)
     *   2. JOIN students through the course_students join table (second level)
     *
     * WARNING: Deep nesting with multiple collections can cause a "Cartesian product"
     * problem where the result set explodes in size. Use with caution.
     */
    @EntityGraph(attributePaths = {"courses", "courses.students"})
    @Query("SELECT DISTINCT d FROM Department d")
    List<Department> findAllWithCoursesAndStudents();

    /**
     * Three levels deep: Department → courses → reviews → student
     */
    @EntityGraph(attributePaths = {"courses", "courses.reviews", "courses.reviews.student"})
    @Query("SELECT DISTINCT d FROM Department d WHERE d.id = :id")
    Optional<Department> findByIdWithFullGraph(Long id);

    // =========================================================================
    // APPROACH 4: JOIN FETCH in JPQL (alternative to EntityGraph)
    // More explicit control but more verbose.
    // =========================================================================

    /**
     * JOIN FETCH alternative: Achieves the same result as @EntityGraph
     * but written directly in JPQL.
     *
     * DISTINCT is needed because the JOIN produces duplicate Department rows
     * (one per course). DISTINCT deduplicates in the application layer.
     *
     * PRO: More explicit, works with any JPA provider, can add WHERE clauses.
     * CON: More verbose, harder to reuse, must remember DISTINCT.
     */
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.courses")
    List<Department> findAllWithCoursesJoinFetch();

    /**
     * Multiple JOIN FETCH: equivalent to @EntityGraph(attributePaths = {"courses", "details"})
     */
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.courses LEFT JOIN FETCH d.details")
    List<Department> findAllWithCoursesAndDetailsJoinFetch();

    // =========================================================================
    // NO EntityGraph — for comparison (will cause N+1)
    // =========================================================================

    /**
     * Plain findAll — NO eager fetching.
     * Accessing department.getCourses() for each will trigger N+1 queries.
     * This method exists to DEMONSTRATE the problem.
     */
    // findAll() is inherited from JpaRepository — it has no EntityGraph.
}
