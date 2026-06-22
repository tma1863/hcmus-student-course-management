package com.example.studentcoursemanagement.common.util;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  public LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  public LocalDateTime updatedAt;
}
