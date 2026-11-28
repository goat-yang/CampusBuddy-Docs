package com.shu.campusbuddy.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campusbuddy")
public class CampusBuddyProperties {

  private final Jwt jwt = new Jwt();
  private final Otp otp = new Otp();
  private final Mail mail = new Mail();
  private final Wechat wechat = new Wechat();
  private final Demo demo = new Demo();
  private String corsOrigins = "http://localhost:5173,http://localhost:5174";

  public Jwt getJwt() {
    return jwt;
  }

  public Otp getOtp() {
    return otp;
  }

  public Mail getMail() {
    return mail;
  }

  public Wechat getWechat() {
    return wechat;
  }

  public Demo getDemo() {
    return demo;
  }

  public String getCorsOrigins() {
    return corsOrigins;
  }

  public void setCorsOrigins(String corsOrigins) {
    this.corsOrigins = corsOrigins;
  }

  public static class Jwt {
    private String secret = "";
    private int accessTtlSec = 7200;
    private int refreshTtlSec = 1_209_600;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public int getAccessTtlSec() {
      return accessTtlSec;
    }

    public void setAccessTtlSec(int accessTtlSec) {
      this.accessTtlSec = accessTtlSec;
    }

    public int getRefreshTtlSec() {
      return refreshTtlSec;
    }

    public void setRefreshTtlSec(int refreshTtlSec) {
      this.refreshTtlSec = refreshTtlSec;
    }
  }

  public static class Otp {
    private int ttlSec = 300;
    private int resendSec = 60;
    /** Blank keeps the in-memory store. Set to use Redis, for example 127.0.0.1. */
    private String redisHost = "";
    private int redisPort = 6379;

    public int getTtlSec() {
      return ttlSec;
    }

    public void setTtlSec(int ttlSec) {
      this.ttlSec = ttlSec;
    }

    public int getResendSec() {
      return resendSec;
    }

    public void setResendSec(int resendSec) {
      this.resendSec = resendSec;
    }

    public String getRedisHost() {
      return redisHost;
    }

    public void setRedisHost(String redisHost) {
      this.redisHost = redisHost;
    }

    public int getRedisPort() {
      return redisPort;
    }

    public void setRedisPort(int redisPort) {
      this.redisPort = redisPort;
    }
  }

  public static class Mail {
    private String from = "noreply@localhost";

    public String getFrom() {
      return from;
    }

    public void setFrom(String from) {
      this.from = from;
    }
  }

  public static class Wechat {
    private String appId = "";
    private String secret = "";

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }
  }

  public static class Demo {
    private String campusId = "";
    private String password = "";

    public String getCampusId() {
      return campusId;
    }

    public void setCampusId(String campusId) {
      this.campusId = campusId;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
