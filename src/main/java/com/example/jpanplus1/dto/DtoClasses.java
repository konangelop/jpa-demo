package com.example.jpanplus1.dto;

import java.util.List;

/**
 * DTOs to avoid exposing entities directly and prevent
 * lazy-loading issues in JSON serialization.
 *
 * BEST PRACTICE: Always use DTOs for API responses.
 * Returning entities directly causes:
 * 1. LazyInitializationException if session is closed
 * 2. Infinite recursion in bidirectional relationships
 * 3. Exposing internal model details to clients
 */
public class DtoClasses {

    public record DepartmentDto(
            Long id,
            String name,
            String description,
            List<CourseSimpleDto> courses,
            DepartmentDetailsDto details
    ) {}

    public record DepartmentSimpleDto(
            Long id,
            String name,
            String description
    ) {}

    public record DepartmentDetailsDto(
            String building,
            Double budget,
            String headOfDepartment
    ) {}

    public record CourseDto(
            Long id,
            String title,
            String description,
            Integer credits,
            DepartmentSimpleDto department,
            List<ReviewDto> reviews,
            List<StudentSimpleDto> students
    ) {}

    public record CourseSimpleDto(
            Long id,
            String title,
            Integer credits
    ) {}

    public record StudentDto(
            Long id,
            String firstName,
            String lastName,
            String email,
            StudentProfileDto profile,
            List<CourseSimpleDto> enrolledCourses
    ) {}

    public record StudentSimpleDto(
            Long id,
            String firstName,
            String lastName,
            String email
    ) {}

    public record StudentProfileDto(
            String bio,
            String phoneNumber,
            String address
    ) {}

    public record ReviewDto(
            Long id,
            String content,
            Integer rating,
            String courseName,
            String studentName
    ) {}

    public record QueryStatsDto(
            String scenario,
            String description,
            long queryCount,
            long executionTimeMs,
            List<?> results
    ) {}
}
