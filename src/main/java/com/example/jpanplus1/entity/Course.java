package com.example.jpanplus1.entity;

import jakarta.persistence.*;
import java.util.*;

/**
 * Course entity demonstrating:
 * - @ManyToOne (Course → Department): The FK-owning side. Most efficient relationship.
 * - @OneToMany (Course → Review): Parent side of reviews.
 * - @ManyToMany (Course ↔ Student): Using a Set for better performance.
 *
 * BEST PRACTICES APPLIED:
 * 1. @ManyToOne defaults to EAGER — we override to LAZY.
 * 2. @ManyToMany uses Set instead of List to avoid the "bag" performance problem.
 * 3. All bidirectional relationships have sync helpers on the parent side.
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    private Integer credits;

    /**
     * MANY-TO-ONE RELATIONSHIP (Best Practice Implementation)
     *
     * - This is the OWNING side (has the FK column: department_id).
     * - fetch = LAZY: CRITICAL! @ManyToOne defaults to EAGER, causing N+1.
     *   Always override to LAZY.
     * - @JoinColumn: Explicitly names the FK column for clarity.
     *
     * WHY LAZY? If we load 100 courses and each eagerly fetches its department,
     * that's potentially 100 extra queries (the "N+1 problem").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * ONE-TO-MANY to Reviews.
     * Same pattern as Department → Course.
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    /**
     * MANY-TO-MANY RELATIONSHIP (Best Practice Implementation)
     *
     * - Uses Set<Student> instead of List<Student>.
     *   WHY? With List, Hibernate treats it as a "bag" (unordered, allows duplicates).
     *   When removing an element from a bag-mapped @ManyToMany, Hibernate:
     *     1. DELETEs ALL rows from the join table for this course
     *     2. Re-INSERTs all remaining associations
     *   With Set, Hibernate only deletes the specific join table row. MUCH more efficient.
     *
     * - @JoinTable: Explicitly defines the join table and columns.
     * - Course is the OWNING side of the ManyToMany (it defines @JoinTable).
     *
     * PERFORMANCE NOTE:
     * For very large @ManyToMany relationships, consider replacing with an explicit
     * join entity (CourseEnrollment) with @ManyToOne on each side. This allows you to
     * add extra columns (enrollment date, grade) and gives finer control.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "course_students",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> students = new HashSet<>();

    // === Constructors ===

    protected Course() {
    }

    public Course(String title, String description, Integer credits) {
        this.title = title;
        this.description = description;
        this.credits = credits;
    }

    // === Bidirectional Sync Helpers ===

    public void addReview(Review review) {
        reviews.add(review);
        review.setCourse(this);
    }

    public void removeReview(Review review) {
        reviews.remove(review);
        review.setCourse(null);
    }

    /**
     * ManyToMany sync: must update BOTH sides for the in-memory model to be consistent.
     * The owning side (Course) manages the join table writes.
     */
    public void enrollStudent(Student student) {
        students.add(student);
        student.getCourses().add(this);
    }

    public void unenrollStudent(Student student) {
        students.remove(student);
        student.getCourses().remove(this);
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public Set<Student> getStudents() {
        return students;
    }

    // === equals/hashCode ===

    /**
     * For entities in a Set (used in Student.courses), proper equals/hashCode is critical.
     * We use title + department name as the business key.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(title, course.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public String toString() {
        return "Course{id=" + id + ", title='" + title + "', credits=" + credits + "}";
    }
}
