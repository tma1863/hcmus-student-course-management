package com.example.studentcoursemanagement.student.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import com.example.studentcoursemanagement.student.enums.EGender;
import com.example.studentcoursemanagement.student.enums.EStudentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Builder(builderMethodName = "builder") // Tự sinh toàn bộ Builder Pattern
@NoArgsConstructor // Bắt buộc cho JPA (Constructor 0 tham số)
@AllArgsConstructor // Bắt buộc để @Builder hoạt động
public class Student extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  public UUID id;

  // Đây là Business Key (Ví dụ: "21280001"), không phải là Primary Key, nhưng vẫn cần unique
  // constraint để đảm bảo không có 2 sinh viên cùng mã sinh viên.
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
  @JoinColumn(name = "major_program_id", nullable = false, updatable = false)
  public MajorProgram majorProgram;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull
  @Builder.Default
  public EStudentStatus status = EStudentStatus.STUDYING;

  @Column(nullable = false, precision = 3, scale = 2)
  @Builder.Default
  public BigDecimal gpa = BigDecimal.ZERO;
}
