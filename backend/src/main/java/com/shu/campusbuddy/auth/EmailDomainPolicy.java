package com.shu.campusbuddy.auth;

import java.util.Locale;
import java.util.Set;

public final class EmailDomainPolicy {

  static final Set<String> DOMAINS =
      Set.of(
          "shu.edu.cn",
          "staff.shu.edu.cn",
          "mail.shu.edu.cn",
          "oa.shu.edu.cn",
          "department.shu.edu.cn");

  private EmailDomainPolicy() {}

  public static boolean allowed(String email) {
    if (email == null) {
      return false;
    }
    int at = email.lastIndexOf('@');
    if (at <= 0 || at == email.length() - 1) {
      return false;
    }
    String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
    return DOMAINS.contains(domain);
  }

  public static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public static String domain(String email) {
    int at = email.lastIndexOf('@');
    if (at < 0 || at == email.length() - 1) {
      return "";
    }
    return email.substring(at + 1).toLowerCase(Locale.ROOT);
  }
}
