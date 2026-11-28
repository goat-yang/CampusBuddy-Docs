package com.shu.campusbuddy.common;

import java.util.UUID;

public final class RequestContext {

  private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

  private RequestContext() {}

  public static void set(String requestId) {
    HOLDER.set(requestId);
  }

  public static String id() {
    String current = HOLDER.get();
    if (current == null) {
      current = UUID.randomUUID().toString();
      HOLDER.set(current);
    }
    return current;
  }

  public static void clear() {
    HOLDER.remove();
  }
}
