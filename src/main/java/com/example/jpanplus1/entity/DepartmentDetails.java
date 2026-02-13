package com.example.jpanplus1.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * DepartmentDetails entity demonstrating:
 * - @OneToOne with @MapsId (shared primary key pattern)
 *
 * BEST PRACTICE: @MapsId for @OneToOne
 *
 * Instead of having a separate auto-generated PK AND a FK column,
 * @MapsId makes the PK of DepartmentDetails be the SAME as Department's PK.
 * This eliminates the extra FK column, saves space, and makes the relationship
 * unambiguous. The PK IS the FK.
 *
 * Table structure with @MapsId:
 *   department_details(department_id PK/FK, building, budget, ...)
 *
 * Without @MapsId (BAD):
 *   department_details(id PK, department_id FK UNIQUE, building, budget, ...)
 *   ↑ Extra column, extra unique constraint, wasteful
 */
@Entity
@Table(name = "department_details")
public class DepartmentDetails {

    /**
     * The ID is the same as the Department's ID.
     * We don't use @GeneratedValue because @MapsId derives it from the association.
     */
    @Id
    private Long id;

    /**
     * @MapsId tells JPA that the PK of this entity is derived from the
     * associated Department entity. The 'id' field will be populated with
     * department.getId() when persisting.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "department_id")
    private Department department;

    private String building;

    private Double budget;

    @Column(name = "head_of_department")
    private String headOfDepartment;

    // === Constructors ===

    protected DepartmentDetails() {
    }

    public DepartmentDetails(String building, Double budget, String headOfDepartment) {
        this.building = building;
        this.budget = budget;
        this.headOfDepartment = headOfDepartment;
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public String getHeadOfDepartment() {
        return headOfDepartment;
    }

    public void setHeadOfDepartment(String headOfDepartment) {
        this.headOfDepartment = headOfDepartment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentDetails that = (DepartmentDetails) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DepartmentDetails{id=" + id + ", building='" + building + "'}";
    }
}
