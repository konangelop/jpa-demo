package com.example.jpanplus1;

import com.example.jpanplus1.config.QueryCounter;
import com.example.jpanplus1.entity.*;
import com.example.jpanplus1.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that PROVE the N+1 problem exists and that
 * EntityGraph/JOIN FETCH solves it by comparing query counts.
 *
 * Each test:
 * 1. Clears the Hibernate statistics
 * 2. Executes a repository method
 * 3. Accesses lazy associations to trigger loading
 * 4. Asserts the number of SQL statements executed
 */
@SpringBootTest
@Transactional
class NPlus1DemoTests {

    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private QueryCounter queryCounter;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Flush and clear the persistence context to ensure clean state
        entityManager.flush();
        entityManager.clear();
        queryCounter.clear();
    }

    // =========================================================================
    // TEST: OneToMany N+1 Problem (Department → Courses)
    // =========================================================================

    @Test
    @DisplayName("N+1 PROBLEM: findAll() + accessing courses triggers N extra queries")
    void demonstrateNPlus1Problem() {
        queryCounter.clear();

        // Query 1: SELECT * FROM departments
        List<Department> departments = departmentRepository.findAll();
        assertThat(departments).isNotEmpty();

        // N additional queries: SELECT * FROM courses WHERE department_id = ? (one per dept)
        departments.forEach(d -> {
            assertThat(d.getCourses()).isNotNull();
            d.getCourses().size(); // Force initialization of lazy collection
        });

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== N+1 PROBLEM: " + queryCount + " queries for " + departments.size() + " departments ===");

        // With 5 departments: should be 6 queries (1 + 5)
        assertThat(queryCount).isGreaterThan(departments.size());
    }

    @Test
    @DisplayName("SOLUTION: @EntityGraph fetches departments + courses in 1 query")
    void entityGraphSolvesNPlus1() {
        queryCounter.clear();

        List<Department> departments = departmentRepository.findAllWithCourses();
        assertThat(departments).isNotEmpty();

        // No additional queries — courses are already loaded!
        departments.forEach(d -> {
            assertThat(d.getCourses()).isNotNull();
            d.getCourses().size();
        });

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== EntityGraph SOLUTION: " + queryCount + " query for " + departments.size() + " departments ===");

        // Should be exactly 1 query
        assertThat(queryCount).isEqualTo(1);
    }

    @Test
    @DisplayName("SOLUTION: JOIN FETCH achieves same result as EntityGraph")
    void joinFetchSolvesNPlus1() {
        queryCounter.clear();

        List<Department> departments = departmentRepository.findAllWithCoursesJoinFetch();
        departments.forEach(d -> d.getCourses().size());

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== JOIN FETCH SOLUTION: " + queryCount + " query ===");

        assertThat(queryCount).isEqualTo(1);
    }

    // =========================================================================
    // TEST: ManyToOne N+1 Problem (Course → Department)
    // =========================================================================

    @Test
    @DisplayName("N+1 PROBLEM: Loading courses and accessing department for each")
    void manyToOneNPlus1() {
        queryCounter.clear();

        List<Course> courses = courseRepository.findAll();
        courses.forEach(c -> c.getDepartment().getName()); // Triggers lazy load

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== ManyToOne N+1: " + queryCount + " queries for " + courses.size() + " courses ===");

        // More than 1 query means N+1 is happening
        assertThat(queryCount).isGreaterThan(1);
    }

    @Test
    @DisplayName("SOLUTION: EntityGraph for ManyToOne")
    void manyToOneEntityGraph() {
        queryCounter.clear();

        List<Course> courses = courseRepository.findAllWithDepartment();
        courses.forEach(c -> c.getDepartment().getName());

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== ManyToOne EntityGraph: " + queryCount + " query ===");

        assertThat(queryCount).isEqualTo(1);
    }

    // =========================================================================
    // TEST: Dual ManyToOne N+1 (Review → Course + Student)
    // =========================================================================

    @Test
    @DisplayName("N+1 PROBLEM: Reviews accessing both course and student")
    void dualManyToOneNPlus1() {
        queryCounter.clear();

        List<Review> reviews = reviewRepository.findAll();
        reviews.forEach(r -> {
            r.getCourse().getTitle();
            r.getStudent().getFirstName();
        });

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== Dual ManyToOne N+1: " + queryCount + " queries for " + reviews.size() + " reviews ===");

        assertThat(queryCount).isGreaterThan(1);
    }

    @Test
    @DisplayName("SOLUTION: EntityGraph fetching both ManyToOne associations")
    void dualManyToOneEntityGraph() {
        queryCounter.clear();

        List<Review> reviews = reviewRepository.findAllWithCourseAndStudent();
        reviews.forEach(r -> {
            r.getCourse().getTitle();
            r.getStudent().getFirstName();
        });

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== Dual ManyToOne EntityGraph: " + queryCount + " query ===");

        assertThat(queryCount).isEqualTo(1);
    }

    // =========================================================================
    // TEST: ManyToMany N+1 (Student → Courses)
    // =========================================================================

    @Test
    @DisplayName("N+1 PROBLEM: Students accessing enrolled courses")
    void manyToManyNPlus1() {
        queryCounter.clear();

        List<Student> students = studentRepository.findAll();
        students.forEach(s -> s.getCourses().size());

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== ManyToMany N+1: " + queryCount + " queries for " + students.size() + " students ===");

        assertThat(queryCount).isGreaterThan(1);
    }

    @Test
    @DisplayName("SOLUTION: EntityGraph for ManyToMany")
    void manyToManyEntityGraph() {
        queryCounter.clear();

        List<Student> students = studentRepository.findAllWithCourses();
        students.forEach(s -> s.getCourses().size());

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== ManyToMany EntityGraph: " + queryCount + " query ===");

        assertThat(queryCount).isEqualTo(1);
    }

    // =========================================================================
    // TEST: Multiple associations with EntityGraph
    // =========================================================================

    @Test
    @DisplayName("EntityGraph: Multiple attributePaths in single query")
    void multipleAssociationsEntityGraph() {
        queryCounter.clear();

        List<Department> departments = departmentRepository.findAllWithCoursesAndDetails();
        departments.forEach(d -> {
            d.getCourses().size();
            if (d.getDetails() != null) {
                d.getDetails().getBuilding();
            }
        });

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== Multi-path EntityGraph: " + queryCount + " query for courses + details ===");

        assertThat(queryCount).isEqualTo(1);
    }

    // =========================================================================
    // TEST: Nested EntityGraph (Department → Courses → Students)
    // =========================================================================

    @Test
    @DisplayName("Nested EntityGraph: Three-level deep fetch")
    void nestedEntityGraph() {
        queryCounter.clear();

        List<Department> departments = departmentRepository.findAllWithCoursesAndStudents();
        departments.forEach(d -> {
            d.getCourses().forEach(c -> {
                c.getStudents().size();
            });
        });

        long queryCount = queryCounter.getPrepareStatementCount();
        System.out.println("=== Nested EntityGraph: " + queryCount + " query for dept → courses → students ===");

        assertThat(queryCount).isEqualTo(1);
    }

    // =========================================================================
    // TEST: Verify data integrity
    // =========================================================================

    @Test
    @DisplayName("Verify sample data is loaded correctly")
    void verifyDataIntegrity() {
        assertThat(departmentRepository.count()).isEqualTo(5);
        assertThat(courseRepository.count()).isEqualTo(16);
        assertThat(studentRepository.count()).isEqualTo(8);
        assertThat(reviewRepository.count()).isEqualTo(20);
    }
}
