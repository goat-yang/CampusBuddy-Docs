package com.shu.campusbuddy.auth;

import java.io.Serializable;
import java.util.Objects;

public class UserTagKey implements Serializable {

  private Long userId;
  private Long tagId;

  public UserTagKey() {}

  public UserTagKey(Long userId, Long tagId) {
    this.userId = userId;
    this.tagId = tagId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof UserTagKey key)) {
      return false;
    }
    return Objects.equals(userId, key.userId) && Objects.equals(tagId, key.tagId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, tagId);
  }
}
