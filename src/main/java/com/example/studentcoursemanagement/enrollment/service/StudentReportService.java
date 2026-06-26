package com.example.studentcoursemanagement.enrollment.service;

import com.example.studentcoursemanagement.enrollment.dto.response.StudentReportResponse;
import java.util.UUID;

public interface StudentReportService {

  /** Builds the student's academic report (transcript) from their enrollments. */
  StudentReportResponse getReport(UUID studentId);
}
