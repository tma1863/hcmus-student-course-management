package com.example.studentcoursemanagement.major.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.student.entity.Student;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(
        name = "majors",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"major_code", "entry_year"})
        }
)
public class Major extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(length = 2, nullable = false)
    @NotNull
    public String majorCode;

    @Column(nullable = false)
    @NotNull
    public String name;

    @Column(length = 4, nullable = false)
    @NotNull
    public String entryYear;

    @Column(nullable = false)
    public Integer totalRequiredCredit;

    @Column(nullable = false)
    public Integer totalOptionalCredit;

    @OneToMany(mappedBy = "major", fetch = FetchType.LAZY)
    public List<Student> students;
}