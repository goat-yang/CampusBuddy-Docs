package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import java.util.Optional;

public final class AuthContext {

  private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

  private AuthContext() {}

  static void set(CurrentUser user) {
    HOLDER.set(user);
  }

  public static Optional<CurrentUser> find() {
    return Optional.ofNullable(HOLDER.get());
  }

  public static CurrentUser require() {
    CurrentUser user = HOLDER.get();
    if (user == null) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
    return user;
  }

  static void clear() {
    HOLDER.remove();
  }
}
