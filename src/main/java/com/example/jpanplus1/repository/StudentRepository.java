package com.example.jpanplus1.repository;

import com.example.jpanplus1.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Students with their enrolled courses.
     */
    @EntityGraph(attributePaths = {"courses"})
    @Query("SELECT DISTINCT s FROM Student s")
    List<Student> findAllWithCourses();

    /**
     * Student with profile and courses — full student view.
     */
    @EntityGraph(attributePaths = {"profile", "courses"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findByIdWithProfileAndCourses(Long id);

    /**
     * Students with profile only.
     */
    @EntityGraph(attributePaths = {"profile"})
    @Query("SELECT s FROM Student s")
    List<Student> findAllWithProfile();
}
