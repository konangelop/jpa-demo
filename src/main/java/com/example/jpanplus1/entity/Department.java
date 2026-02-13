package com.example.jpanplus1.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Department entity demonstrating:
 * - @OneToMany (Department → Course): The parent side, mapped by the child.
 * - @OneToOne with shared primary key (Department → DepartmentDetails): Using @MapsId.
 *
 * BEST PRACTICES APPLIED:
 * 1. All collections use LAZY fetching (JPA default for collections).
 * 2. Collections initialized to empty ArrayList to avoid NullPointerException.
 * 3. Bidirectional helper methods (addCourse/removeCourse) to keep both sides in sync.
 * 4. equals/hashCode based on business key (name), NOT on generated ID.
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /**
     * ONE-TO-MANY RELATIONSHIP (Best Practice Implementation)
     *
     * - mappedBy = "department": The Course entity owns the FK. Department is the inverse side.
     * - CascadeType.ALL: Lifecycle of courses is managed through the department.
     * - orphanRemoval = true: Removing a course from this list will DELETE it from DB.
     * - fetch = LAZY (default for collections): Courses are NOT loaded when Department is fetched.
     *
     * WHY mappedBy? Without it, JPA creates a join table (department_courses) instead of a FK
     * column in the courses table. This is less efficient and produces extra INSERT/DELETE
     * statements on the join table.
     */
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    /**
     * ONE-TO-ONE RELATIONSHIP (Best Practice: Child owns FK with @MapsId)
     *
     * - mappedBy = "department": DepartmentDetails owns the relationship.
     * - CascadeType.ALL: Details lifecycle managed through Department.
     * - fetch = LAZY: CRITICAL! @OneToOne is EAGER by default. We override to LAZY.
     *
     * NOTE ON @OneToOne LAZY LOADING:
     * Lazy loading on the NON-OWNING side of @OneToOne only works if the relationship
     * is guaranteed to be non-null (optional = false) OR if bytecode enhancement is used.
     * Without these, Hibernate must query to check if a related entity exists, defeating LAZY.
     * Using @MapsId (shared PK) on the owning side is the recommended approach.
     */
    @OneToOne(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private DepartmentDetails details;

    // === Constructors ===

    protected Department() {
        // Required by JPA
    }

    public Department(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // === Bidirectional Sync Helpers ===

    /**
     * CRITICAL: Always use these helper methods instead of directly modifying the list.
     * They keep both sides of the bidirectional relationship in sync.
     */
    public void addCourse(Course course) {
        courses.add(course);
        course.setDepartment(this);
    }

    public void removeCourse(Course course) {
        courses.remove(course);
        course.setDepartment(null);
    }

    public void setDetails(DepartmentDetails details) {
        this.details = details;
        if (details != null) {
            details.setDepartment(this);
        }
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public DepartmentDetails getDetails() {
        return details;
    }

    // === equals/hashCode ===

    /**
     * BEST PRACTICE: Use natural/business key for equals/hashCode.
     * - NEVER use @Id (generated value): It's null before persist, breaking Set/Map behavior.
     * - Using a unique business key (name) ensures consistent behavior across entity states.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Department that = (Department) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Department{id=" + id + ", name='" + name + "'}";
    }
}
