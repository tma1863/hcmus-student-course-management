package com.example.studentcoursemanagement.major.dao;

import com.example.studentcoursemanagement.major.entity.Major;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MajorRepository implements PanacheRepository<Major> {

  public boolean existsByMajorCode(String majorCode) {
    return count("majorCode = ?1", majorCode) > 0;
  }
}
