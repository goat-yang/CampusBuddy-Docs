package com.shu.campusbuddy.auth;

public final class Masks {

  private Masks() {}

  public static String campusId(String id) {
    if (id == null || id.isBlank()) {
      return "";
    }
    if (id.length() <= 4) {
      return "****";
    }
    return id.substring(0, 4) + "****";
  }

  public static String email(String email) {
    if (email == null || email.isBlank()) {
      return "";
    }
    int at = email.indexOf('@');
    if (at <= 0) {
      return "****";
    }
    String local = email.substring(0, at);
    String domain = email.substring(at);
    String masked = local.length() <= 4 ? "****" : local.substring(0, 4) + "****";
    return masked + domain;
  }
}
