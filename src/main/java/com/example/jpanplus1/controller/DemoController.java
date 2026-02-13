package com.example.jpanplus1.controller;

import com.example.jpanplus1.dto.DtoClasses.QueryStatsDto;
import com.example.jpanplus1.service.DemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing endpoints to demonstrate N+1 problem and solutions.
 *
 * Hit each endpoint and observe:
 * 1. The JSON response showing query counts
 * 2. The SQL logs in the console (Hibernate SQL logging is enabled)
 *
 * Recommended exploration order:
 * 1. /api/demo/compare/departments        — OneToMany N+1 vs EntityGraph
 * 2. /api/demo/compare/courses            — ManyToOne N+1 vs EntityGraph
 * 3. /api/demo/compare/reviews            — Dual ManyToOne N+1 vs EntityGraph
 * 4. /api/demo/compare/students           — ManyToMany N+1 vs EntityGraph
 * 5. /api/demo/compare/departments-full   — Multiple associations
 * 6. /api/demo/nested-entitygraph         — Nested (deep) EntityGraph
 * 7. /api/demo/compare/fetch-vs-load      — FETCH vs LOAD graph types
 * 8. /api/demo/all                        — Run ALL scenarios at once
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    // =========================================================================
    // Side-by-side comparisons: N+1 Problem vs EntityGraph Solution
    // =========================================================================

    /**
     * SCENARIO 1: OneToMany — Department → Courses
     * Compare N+1 vs EntityGraph query counts.
     */
    @GetMapping("/compare/departments")
    public Map<String, QueryStatsDto> compareDepartments() {
        Map<String, QueryStatsDto> comparison = new LinkedHashMap<>();
        comparison.put("problem_nplus1", demoService.getDepartmentsWithCoursesNPlus1());
        comparison.put("solution_entitygraph", demoService.getDepartmentsWithCoursesEntityGraph());
        comparison.put("solution_join_fetch", demoService.getDepartmentsWithCoursesJoinFetch());
        return comparison;
    }

    /**
     * SCENARIO 2: ManyToOne — Course → Department
     */
    @GetMapping("/compare/courses")
    public Map<String, QueryStatsDto> compareCourses() {
        Map<String, QueryStatsDto> comparison = new LinkedHashMap<>();
        comparison.put("problem_nplus1", demoService.getCoursesWithDepartmentNPlus1());
        comparison.put("solution_entitygraph", demoService.getCoursesWithDepartmentEntityGraph());
        return comparison;
    }

    /**
     * SCENARIO 3: Dual ManyToOne — Review → Course + Student
     */
    @GetMapping("/compare/reviews")
    public Map<String, QueryStatsDto> compareReviews() {
        Map<String, QueryStatsDto> comparison = new LinkedHashMap<>();
        comparison.put("problem_nplus1", demoService.getReviewsNPlus1());
        comparison.put("solution_entitygraph", demoService.getReviewsEntityGraph());
        return comparison;
    }

    /**
     * SCENARIO 4: ManyToMany — Student → Courses
     */
    @GetMapping("/compare/students")
    public Map<String, QueryStatsDto> compareStudents() {
        Map<String, QueryStatsDto> comparison = new LinkedHashMap<>();
        comparison.put("problem_nplus1", demoService.getStudentsWithCoursesNPlus1());
        comparison.put("solution_entitygraph", demoService.getStudentsWithCoursesEntityGraph());
        return comparison;
    }

    /**
     * SCENARIO 5: Multiple associations — Department → Courses + Details
     */
    @GetMapping("/compare/departments-full")
    public Map<String, QueryStatsDto> compareDepartmentsFull() {
        Map<String, QueryStatsDto> comparison = new LinkedHashMap<>();
        comparison.put("problem_double_nplus1", demoService.getDepartmentsFullNPlus1());
        comparison.put("solution_multi_entitygraph", demoService.getDepartmentsFullEntityGraph());
        return comparison;
    }

    /**
     * SCENARIO 6: FETCH vs LOAD EntityGraph types
     */
    @GetMapping("/compare/fetch-vs-load")
    public Map<String, QueryStatsDto> compareFetchVsLoad() {
        Map<String, QueryStatsDto> comparison = new LinkedHashMap<>();
        comparison.put("fetch_graph", demoService.getDepartmentsFetchGraph());
        comparison.put("load_graph", demoService.getDepartmentsLoadGraph());
        return comparison;
    }

    // =========================================================================
    // Individual endpoints
    // =========================================================================

    @GetMapping("/nested-entitygraph")
    public QueryStatsDto nestedEntityGraph() {
        return demoService.getDepartmentsNestedEntityGraph();
    }

    // =========================================================================
    // Run ALL scenarios
    // =========================================================================

    @GetMapping("/all")
    public Map<String, Object> runAllScenarios() {
        Map<String, Object> allResults = new LinkedHashMap<>();

        allResults.put("1_oneToMany_department_courses", compareDepartments());
        allResults.put("2_manyToOne_course_department", compareCourses());
        allResults.put("3_dual_manyToOne_review", compareReviews());
        allResults.put("4_manyToMany_student_courses", compareStudents());
        allResults.put("5_multiple_associations", compareDepartmentsFull());
        allResults.put("6_nested_entitygraph", nestedEntityGraph());
        allResults.put("7_fetch_vs_load_graph", compareFetchVsLoad());

        return allResults;
    }
}
