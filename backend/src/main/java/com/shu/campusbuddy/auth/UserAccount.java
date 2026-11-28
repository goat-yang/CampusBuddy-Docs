package com.shu.campusbuddy.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import com.shu.campusbuddy.common.BeijingTime;

@Entity
@Table(name = "t_user")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "campus_id", length = 20)
  private String campusId;

  @Column(name = "school_email", length = 128)
  private String schoolEmail;

  @Column(length = 64)
  private String openid;

  @Column(length = 32)
  private String phone;

  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 20)
  private String nickname;

  @Column(length = 512)
  private String avatar;

  @Column(length = 64)
  private String college;

  @Column(length = 32)
  private String grade;

  @Column(length = 64)
  private String major;

  @Column(length = 16)
  private String campus;

  @Column(length = 200)
  private String bio;

  @Column(name = "credit_score", nullable = false)
  private int creditScore = 100;

  @Column(name = "user_role", nullable = false, length = 16)
  private String role;

  @Column(length = 8)
  private String affiliation;

  @Column(name = "account_status", nullable = false, length = 32)
  private String accountStatus = "NORMAL";

  @Column(nullable = false)
  private boolean deleted;

  @Column(name = "create_time", nullable = false)
  private LocalDateTime createTime;

  @Column(name = "update_time", nullable = false)
  private LocalDateTime updateTime;

  @PrePersist
  void creating() {
    LocalDateTime now = LocalDateTime.now(BeijingTime.ZONE);
    createTime = now;
    updateTime = now;
    if (role == null) {
      role = "USER";
    }
    if (accountStatus == null) {
      accountStatus = "NORMAL";
    }
  }

  @PreUpdate
  void updating() {
    updateTime = LocalDateTime.now(BeijingTime.ZONE);
  }

  public Long getId() {
    return id;
  }

  public String getCampusId() {
    return campusId;
  }

  public void setCampusId(String campusId) {
    this.campusId = campusId;
  }

  public String getSchoolEmail() {
    return schoolEmail;
  }

  public void setSchoolEmail(String schoolEmail) {
    this.schoolEmail = schoolEmail;
  }

  public String getOpenid() {
    return openid;
  }

  public void setOpenid(String openid) {
    this.openid = openid;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public String getCollege() {
    return college;
  }

  public void setCollege(String college) {
    this.college = college;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public String getMajor() {
    return major;
  }

  public void setMajor(String major) {
    this.major = major;
  }

  public String getCampus() {
    return campus;
  }

  public void setCampus(String campus) {
    this.campus = campus;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public int getCreditScore() {
    return creditScore;
  }

  public void setCreditScore(int creditScore) {
    this.creditScore = creditScore;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(String affiliation) {
    this.affiliation = affiliation;
  }

  public String getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}
