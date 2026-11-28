package com.shu.campusbuddy.common;

public record ApiResponse(int code, String message, Object data, String requestId) {

  public static ApiResponse ok(Object data) {
    return new ApiResponse(0, "ok", data, RequestContext.id());
  }

  public static ApiResponse fail(int code, String message, Object data) {
    return new ApiResponse(code, message, data, RequestContext.id());
  }
}
