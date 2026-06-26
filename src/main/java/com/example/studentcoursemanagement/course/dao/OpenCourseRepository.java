package com.example.studentcoursemanagement.course.dao;

import com.example.studentcoursemanagement.course.entity.OpenCourse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OpenCourseRepository implements PanacheRepository<OpenCourse> {}
