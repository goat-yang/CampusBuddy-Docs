package com.shu.campusbuddy.common;

/** Business error. Message is safe to return and must not contain secrets. */
public class ApiException extends RuntimeException {

  private final int httpStatus;
  private final int code;
  private final String field;
  private final boolean retryable;
  private final Integer retryAfterSec;

  public ApiException(
      int httpStatus, int code, String message, String field, boolean retryable, Integer retryAfterSec) {
    super(message);
    this.httpStatus = httpStatus;
    this.code = code;
    this.field = field;
    this.retryable = retryable;
    this.retryAfterSec = retryAfterSec;
  }

  public static ApiException bad(String field, String message) {
    return new ApiException(400, 40001, message, field, false, null);
  }

  public static ApiException unauthorized(String message) {
    return new ApiException(401, 40100, message, null, false, null);
  }

  public static ApiException forbidden(String message, String field) {
    return new ApiException(403, 40300, message, field, false, null);
  }

  public static ApiException notFound() {
    return new ApiException(404, 40400, "资源不存在", null, false, null);
  }

  public static ApiException conflict(int code, String message, String field) {
    return new ApiException(409, code, message, field, false, null);
  }

  public static ApiException rateLimited(long retryAfterSec) {
    int seconds = (int) Math.max(1, retryAfterSec);
    return new ApiException(429, 42900, "验证码发送过于频繁，请 60 秒后重试", "otp", true, seconds);
  }

  public static ApiException system(String message) {
    return new ApiException(500, 50000, message, null, false, null);
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public int getCode() {
    return code;
  }

  public String getField() {
    return field;
  }

  public boolean isRetryable() {
    return retryable;
  }

  public Integer getRetryAfterSec() {
    return retryAfterSec;
  }
}
