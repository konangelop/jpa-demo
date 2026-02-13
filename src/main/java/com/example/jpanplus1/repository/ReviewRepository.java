package com.example.jpanplus1.repository;

import com.example.jpanplus1.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Reviews with course and student — solves dual N+1 problem.
     * Without EntityGraph: Loading N reviews → N queries for course + N queries for student = 2N+1.
     * With EntityGraph: 1 query with two JOINs.
     */
    @EntityGraph(attributePaths = {"course", "student"})
    @Query("SELECT r FROM Review r")
    List<Review> findAllWithCourseAndStudent();

    /**
     * Even deeper: reviews → course → department, reviews → student
     */
    @EntityGraph(attributePaths = {"course", "course.department", "student"})
    @Query("SELECT r FROM Review r")
    List<Review> findAllWithFullDetails();

    /**
     * Filter by rating with eager fetch.
     */
    @EntityGraph(attributePaths = {"course", "student"})
    List<Review> findByRatingGreaterThanEqual(Integer rating);

    /**
     * JOIN FETCH equivalent for comparison.
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.course JOIN FETCH r.student")
    List<Review> findAllWithCourseAndStudentJoinFetch();
}
