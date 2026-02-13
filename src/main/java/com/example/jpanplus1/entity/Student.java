package com.example.jpanplus1.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Student entity demonstrating:
 * - @ManyToMany inverse side (Student ↔ Course): mapped by Course.
 * - @OneToOne with @MapsId (Student → StudentProfile): Same shared-PK pattern.
 *
 * BEST PRACTICE: The @ManyToMany inverse side uses mappedBy and does NOT
 * define @JoinTable. Only the owning side (Course) manages the join table.
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * MANY-TO-MANY INVERSE SIDE
     *
     * - mappedBy = "students": Course.students is the owning side.
     * - No @JoinTable here — it's defined on Course.
     * - Uses Set for the same performance reasons as Course.students.
     * - NO CascadeType.REMOVE or ALL! Removing a student should NOT delete courses.
     *   Only PERSIST and MERGE are safe for @ManyToMany.
     */
    @ManyToMany(mappedBy = "students")
    private Set<Course> courses = new HashSet<>();

    /**
     * ONE-TO-ONE (mapped by the profile which uses @MapsId)
     */
    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private StudentProfile profile;

    // === Constructors ===

    protected Student() {
    }

    public Student(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // === Bidirectional Sync Helper ===

    public void setProfile(StudentProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setStudent(this);
        }
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Course> getCourses() {
        return courses;
    }

    public StudentProfile getProfile() {
        return profile;
    }

    // === equals/hashCode on business key (email) ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(email, student.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + firstName + " " + lastName + "', email='" + email + "'}";
    }
}
