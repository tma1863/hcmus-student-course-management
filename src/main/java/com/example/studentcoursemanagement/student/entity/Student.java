package com.example.studentcoursemanagement.student.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.major.entity.Major;
import com.example.studentcoursemanagement.student.enums.EGender;
import com.example.studentcoursemanagement.student.enums.EStudentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    public UUID id;

    // Đây là Business Key (Ví dụ: "21280001") dùng để hiển thị và tìm kiếm nhanh
    @Column(name = "student_id", length = 8, nullable = false, updatable = false, unique = true)
    public String studentId;

    @Column(nullable = false)
    @NotNull
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    public EGender gender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "major_id", nullable = false)
    public Major major;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    public EStudentStatus status;

    @Column(nullable = false, precision = 3, scale = 2)
    public BigDecimal gpa;
}