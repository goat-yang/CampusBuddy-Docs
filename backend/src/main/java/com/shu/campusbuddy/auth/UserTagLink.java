package com.shu.campusbuddy.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_user_tag")
@IdClass(UserTagKey.class)
public class UserTagLink {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(name = "tag_id")
  private Long tagId;

  @Column(name = "sort_no", nullable = false)
  private int sortNo;

  @Column(name = "create_time", nullable = false)
  private LocalDateTime createTime;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getTagId() {
    return tagId;
  }

  public void setTagId(Long tagId) {
    this.tagId = tagId;
  }

  public int getSortNo() {
    return sortNo;
  }

  public void setSortNo(int sortNo) {
    this.sortNo = sortNo;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
