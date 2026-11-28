package com.shu.campusbuddy.auth;

public final class CreditLevels {

  private CreditLevels() {}

  public static String of(int score) {
    int bounded = Math.max(0, Math.min(100, score));
    if (bounded >= 90) {
      return "EXCELLENT";
    }
    if (bounded >= 75) {
      return "GOOD";
    }
    if (bounded >= 60) {
      return "NORMAL";
    }
    return "RESTRICTED";
  }
}
