package com.shu.campusbuddy.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_idempotency")
public class IdempotencyRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "idem_key", nullable = false, length = 36)
  private String idemKey;

  @Column(nullable = false, length = 160)
  private String path;

  @Column(name = "body_hash", nullable = false, length = 64)
  private String bodyHash;

  @Column(name = "http_status", nullable = false)
  private int httpStatus;

  @Column(name = "response_body", nullable = false, columnDefinition = "LONGTEXT")
  private String responseBody;

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

  public String getIdemKey() {
    return idemKey;
  }

  public void setIdemKey(String idemKey) {
    this.idemKey = idemKey;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getBodyHash() {
    return bodyHash;
  }

  public void setBodyHash(String bodyHash) {
    this.bodyHash = bodyHash;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }
}
