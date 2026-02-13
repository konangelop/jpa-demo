package com.example.jpanplus1.service;

import com.example.jpanplus1.config.QueryCounter;
import com.example.jpanplus1.dto.DtoClasses.*;
import com.example.jpanplus1.entity.*;
import com.example.jpanplus1.repository.*;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer demonstrating the N+1 problem and various solutions.
 *
 * Each method pair shows:
 *   - xxxWithNPlus1()      → The PROBLEMATIC approach (causes N+1 queries)
 *   - xxxWithEntityGraph()  → The SOLUTION using EntityGraph
 *
 * The QueryCounter tracks exact query counts for comparison.
 */
@Service
@Transactional(readOnly = true)
public class DemoService {

    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final ReviewRepository reviewRepository;
    private final StudentRepository studentRepository;
    private final QueryCounter queryCounter;

    public DemoService(DepartmentRepository departmentRepository,
                       CourseRepository courseRepository,
                       ReviewRepository reviewRepository,
                       StudentRepository studentRepository,
                       QueryCounter queryCounter) {
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.reviewRepository = reviewRepository;
        this.studentRepository = studentRepository;
        this.queryCounter = queryCounter;
    }

    // =========================================================================
    // SCENARIO 1: OneToMany — Department → Courses (N+1 Problem)
    // =========================================================================

    /**
     * THE N+1 PROBLEM:
     *
     * 1 query: SELECT * FROM departments          (loads N departments)
     * N queries: SELECT * FROM courses WHERE department_id = ?  (one per department)
     *
     * Total: N+1 queries. With 5 departments, that's 6 queries.
     * With 1000 departments, that's 1001 queries!
     */
    public QueryStatsDto getDepartmentsWithCoursesNPlus1() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        // Query 1: Load all departments
        List<Department> departments = departmentRepository.findAll();

        // This loop triggers N additional queries (one per department)
        List<DepartmentDto> result = departments.stream()
                .map(d -> new DepartmentDto(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        // TRIGGER: Accessing the lazy collection fires a SELECT for each department
                        d.getCourses().stream()
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList(),
                        null
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        log.warn("⚠️  N+1 PROBLEM: {} departments loaded with {} SQL statements", departments.size(), queries);

        return new QueryStatsDto(
                "N+1 Problem: Department → Courses",
                "findAll() + accessing getCourses() for each department. " +
                        "1 query for departments + N queries for courses = " + queries + " total SQL statements.",
                queries, elapsed, result
        );
    }

    /**
     * SOLUTION: EntityGraph fetches departments AND courses in ONE query.
     *
     * Generated SQL:
     * SELECT d.*, c.* FROM departments d LEFT JOIN courses c ON d.id = c.department_id
     *
     * Total: 1 query regardless of how many departments exist.
     */
    public QueryStatsDto getDepartmentsWithCoursesEntityGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        // Single query with LEFT JOIN FETCH via EntityGraph
        List<Department> departments = departmentRepository.findAllWithCourses();

        // No additional queries — courses are already loaded!
        List<DepartmentDto> result = departments.stream()
                .map(d -> new DepartmentDto(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        d.getCourses().stream()
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList(),
                        null
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        log.info("✅ EntityGraph SOLUTION: {} departments loaded with {} SQL statement(s)", departments.size(), queries);

        return new QueryStatsDto(
                "EntityGraph Solution: Department → Courses",
                "findAllWithCourses() using @EntityGraph(attributePaths={\"courses\"}). " +
                        "Single query with LEFT JOIN. Total: " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    /**
     * SOLUTION VARIANT: JOIN FETCH in JPQL (alternative to EntityGraph)
     */
    public QueryStatsDto getDepartmentsWithCoursesJoinFetch() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Department> departments = departmentRepository.findAllWithCoursesJoinFetch();

        List<DepartmentDto> result = departments.stream()
                .map(d -> new DepartmentDto(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        d.getCourses().stream()
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList(),
                        null
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "JOIN FETCH Solution: Department → Courses",
                "JPQL with JOIN FETCH. Equivalent to EntityGraph. Total: " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    // =========================================================================
    // SCENARIO 2: Multiple associations — Department → Courses + Details
    // =========================================================================

    public QueryStatsDto getDepartmentsFullNPlus1() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Department> departments = departmentRepository.findAll();

        // Accessing BOTH courses AND details triggers 2N additional queries
        List<DepartmentDto> result = departments.stream()
                .map(d -> new DepartmentDto(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        d.getCourses().stream()
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList(),
                        d.getDetails() != null ?
                                new DepartmentDetailsDto(
                                        d.getDetails().getBuilding(),
                                        d.getDetails().getBudget(),
                                        d.getDetails().getHeadOfDepartment()
                                ) : null
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        log.warn("⚠️  DOUBLE N+1: {} SQL statements for {} departments (courses + details)", queries, departments.size());

        return new QueryStatsDto(
                "Double N+1: Department → Courses + Details",
                "findAll() + accessing getCourses() AND getDetails(). " +
                        "1 + N (courses) + N (details) = " + queries + " total SQL statements.",
                queries, elapsed, result
        );
    }

    public QueryStatsDto getDepartmentsFullEntityGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Department> departments = departmentRepository.findAllWithCoursesAndDetails();

        List<DepartmentDto> result = departments.stream()
                .map(d -> new DepartmentDto(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        d.getCourses().stream()
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList(),
                        d.getDetails() != null ?
                                new DepartmentDetailsDto(
                                        d.getDetails().getBuilding(),
                                        d.getDetails().getBudget(),
                                        d.getDetails().getHeadOfDepartment()
                                ) : null
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "EntityGraph Solution: Department → Courses + Details",
                "@EntityGraph(attributePaths={\"courses\",\"details\"}). " +
                        "Single query with two JOINs. Total: " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    // =========================================================================
    // SCENARIO 3: ManyToOne — Course → Department (N+1)
    // =========================================================================

    public QueryStatsDto getCoursesWithDepartmentNPlus1() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Course> courses = courseRepository.findAll();

        // Accessing getDepartment().getName() for each course triggers N queries
        List<CourseDto> result = courses.stream()
                .map(c -> new CourseDto(
                        c.getId(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getCredits(),
                        new DepartmentSimpleDto(
                                c.getDepartment().getId(),
                                c.getDepartment().getName(),
                                c.getDepartment().getDescription()
                        ),
                        List.of(),
                        List.of()
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "N+1 Problem: Course → Department (@ManyToOne)",
                "findAll() + getDepartment() per course. Total: " + queries + " SQL statements.",
                queries, elapsed, result
        );
    }

    public QueryStatsDto getCoursesWithDepartmentEntityGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Course> courses = courseRepository.findAllWithDepartment();

        List<CourseDto> result = courses.stream()
                .map(c -> new CourseDto(
                        c.getId(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getCredits(),
                        new DepartmentSimpleDto(
                                c.getDepartment().getId(),
                                c.getDepartment().getName(),
                                c.getDepartment().getDescription()
                        ),
                        List.of(),
                        List.of()
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "EntityGraph Solution: Course → Department",
                "@EntityGraph(attributePaths={\"department\"}). Total: " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    // =========================================================================
    // SCENARIO 4: ManyToOne from both sides — Review → Course + Student
    // =========================================================================

    public QueryStatsDto getReviewsNPlus1() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Review> reviews = reviewRepository.findAll();

        // Double N+1: accessing both course and student for each review
        List<ReviewDto> result = reviews.stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getContent(),
                        r.getRating(),
                        r.getCourse().getTitle(),       // triggers query
                        r.getStudent().getFirstName()    // triggers another query
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "Double N+1: Review → Course + Student",
                "For each review: 1 query for course + 1 query for student. Total: " + queries + " SQL statements.",
                queries, elapsed, result
        );
    }

    public QueryStatsDto getReviewsEntityGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Review> reviews = reviewRepository.findAllWithCourseAndStudent();

        List<ReviewDto> result = reviews.stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getContent(),
                        r.getRating(),
                        r.getCourse().getTitle(),
                        r.getStudent().getFirstName()
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "EntityGraph Solution: Review → Course + Student",
                "@EntityGraph(attributePaths={\"course\",\"student\"}). Total: " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    // =========================================================================
    // SCENARIO 5: ManyToMany — Student → Courses
    // =========================================================================

    public QueryStatsDto getStudentsWithCoursesNPlus1() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Student> students = studentRepository.findAll();

        List<StudentDto> result = students.stream()
                .map(s -> new StudentDto(
                        s.getId(),
                        s.getFirstName(),
                        s.getLastName(),
                        s.getEmail(),
                        null,
                        s.getCourses().stream() // triggers N+1
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList()
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "N+1 Problem: Student → Courses (@ManyToMany)",
                "findAll() + getCourses() per student. Total: " + queries + " SQL statements.",
                queries, elapsed, result
        );
    }

    public QueryStatsDto getStudentsWithCoursesEntityGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Student> students = studentRepository.findAllWithCourses();

        List<StudentDto> result = students.stream()
                .map(s -> new StudentDto(
                        s.getId(),
                        s.getFirstName(),
                        s.getLastName(),
                        s.getEmail(),
                        null,
                        s.getCourses().stream()
                                .map(c -> new CourseSimpleDto(c.getId(), c.getTitle(), c.getCredits()))
                                .toList()
                ))
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "EntityGraph Solution: Student → Courses (@ManyToMany)",
                "@EntityGraph(attributePaths={\"courses\"}). Total: " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    // =========================================================================
    // SCENARIO 6: Nested EntityGraph — Department → Courses → Students
    // =========================================================================

    public QueryStatsDto getDepartmentsNestedEntityGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        List<Department> departments = departmentRepository.findAllWithCoursesAndStudents();

        List<Map<String, Object>> result = departments.stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("department", d.getName());
                    map.put("courses", d.getCourses().stream()
                            .map(c -> {
                                Map<String, Object> courseMap = new LinkedHashMap<>();
                                courseMap.put("title", c.getTitle());
                                courseMap.put("students", c.getStudents().stream()
                                        .map(s -> s.getFirstName() + " " + s.getLastName())
                                        .toList());
                                return courseMap;
                            })
                            .toList());
                    return map;
                })
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "Nested EntityGraph: Department → Courses → Students",
                "@EntityGraph(attributePaths={\"courses\",\"courses.students\"}). " +
                        "Three-level deep fetch in " + queries + " SQL statement(s).",
                queries, elapsed, result
        );
    }

    // =========================================================================
    // SCENARIO 7: FETCH vs LOAD EntityGraph comparison
    // =========================================================================

    public QueryStatsDto getDepartmentsFetchGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        // FETCH graph: Only 'courses' is eager, everything else is LAZY
        List<Department> departments = departmentRepository.findAllWithCourses();

        long fetchQueries = queryCounter.getPrepareStatementCount();

        // Accessing details will trigger additional query (not in FETCH graph)
        departments.forEach(d -> {
            if (d.getDetails() != null) {
                d.getDetails().getBuilding(); // Extra query per department!
            }
        });

        long totalQueries = queryCounter.getPrepareStatementCount();
        long elapsed = System.currentTimeMillis() - start;

        return new QueryStatsDto(
                "FETCH Graph: courses eager, details lazy",
                "FETCH graph loaded courses in " + fetchQueries + " statement(s). " +
                        "Accessing details caused " + (totalQueries - fetchQueries) + " extra queries. " +
                        "Total: " + totalQueries + " SQL statements.",
                totalQueries, elapsed, List.of()
        );
    }

    public QueryStatsDto getDepartmentsLoadGraph() {
        queryCounter.clear();
        long start = System.currentTimeMillis();

        // LOAD graph: 'courses' is eager, others use their entity-defined fetch type
        List<Department> departments = departmentRepository.findAllWithCoursesLoadGraph();

        long elapsed = System.currentTimeMillis() - start;
        long queries = queryCounter.getPrepareStatementCount();

        return new QueryStatsDto(
                "LOAD Graph: courses eager, others use entity defaults",
                "LOAD graph with attributePaths={\"courses\"}. " +
                        "Total: " + queries + " SQL statement(s). " +
                        "Other associations use their entity-defined FetchType.",
                queries, elapsed, List.of()
        );
    }
}
