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
@Table(name = "t_refresh_token")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false, length = 36)
  private String jti;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  @Column(name = "create_time", nullable = false)
  private LocalDateTime createTime;

  @PrePersist
  void creating() {
    if (createTime == null) {
      createTime = LocalDateTime.now(BeijingTime.ZONE);
    }
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getJti() {
    return jti;
  }

  public void setJti(String jti) {
    this.jti = jti;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }
}
