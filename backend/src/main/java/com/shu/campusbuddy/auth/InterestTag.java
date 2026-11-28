package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.BeijingTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_tag")
public class InterestTag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 32)
  private String name;

  @Column(nullable = false, length = 32)
  private String category;

  @Column(nullable = false)
  private boolean deleted;

  @Column(name = "create_time", nullable = false)
  private LocalDateTime createTime;

  @Column(name = "update_time", nullable = false)
  private LocalDateTime updateTime;

  @PrePersist
  void creating() {
    LocalDateTime now = LocalDateTime.now(BeijingTime.ZONE);
    if (createTime == null) {
      createTime = now;
    }
    if (updateTime == null) {
      updateTime = now;
    }
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public boolean isDeleted() {
    return deleted;
  }
}
