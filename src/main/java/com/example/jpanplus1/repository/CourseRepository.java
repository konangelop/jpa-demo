package com.example.jpanplus1.repository;

import com.example.jpanplus1.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // =========================================================================
    // EntityGraph examples for Course
    // =========================================================================

    /**
     * Fetch courses with their department in one query.
     * Solves the classic N+1: loading all courses then accessing getDepartment().getName()
     */
    @EntityGraph(attributePaths = {"department"})
    @Query("SELECT c FROM Course c")
    List<Course> findAllWithDepartment();

    /**
     * Fetch course with ALL associations.
     * Useful for a "course detail" page that shows everything.
     *
     * NOTE: Fetching multiple collections (reviews + students) in one query can
     * cause a Cartesian product. Hibernate will log a warning:
     * "HHH90003004: firstResult/maxResults specified with collection fetch"
     *
     * For production, consider fetching collections separately or using @BatchSize.
     */
    @EntityGraph(attributePaths = {"department", "reviews", "students"})
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithAllAssociations(Long id);

    /**
     * EntityGraph with department + reviews (safe: only one collection).
     */
    @EntityGraph(attributePaths = {"department", "reviews"})
    @Query("SELECT c FROM Course c")
    List<Course> findAllWithDepartmentAndReviews();

    /**
     * EntityGraph with department + students.
     */
    @EntityGraph(attributePaths = {"department", "students"})
    @Query("SELECT DISTINCT c FROM Course c")
    List<Course> findAllWithDepartmentAndStudents();

    // =========================================================================
    // JOIN FETCH alternatives
    // =========================================================================

    @Query("SELECT c FROM Course c JOIN FETCH c.department")
    List<Course> findAllWithDepartmentJoinFetch();

    @Query("SELECT DISTINCT c FROM Course c JOIN FETCH c.department LEFT JOIN FETCH c.reviews")
    List<Course> findAllWithDepartmentAndReviewsJoinFetch();

    // =========================================================================
    // Named EntityGraph (defined on entity class — see alternative approach in README)
    // =========================================================================

    // Spring Data also supports: @EntityGraph(value = "Course.withDepartment")
    // This requires @NamedEntityGraph on the Course entity. See README for details.
}
