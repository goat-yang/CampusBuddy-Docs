package com.shu.campusbuddy.common;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse> handleApi(ApiException ex) {
    Object data = ex.getHttpStatus() == 404 ? null : new ErrorData(ex.getField(), ex.isRetryable(), ex.getRetryAfterSec());
    return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponse.fail(ex.getCode(), ex.getMessage(), data));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiResponse> handleHeader(MissingRequestHeaderException ex) {
    String name = ex.getHeaderName();
    return ResponseEntity.badRequest()
        .body(ApiResponse.fail(40001, "缺少 " + name, new ErrorData(name, false, null)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    String field = "body";
    String message = "请求体无法解析";
    Throwable cause = ex.getCause();
    if (cause instanceof UnrecognizedPropertyException unknown) {
      field = unknown.getPropertyName();
      message = field + " 不允许";
    }
    return ResponseEntity.badRequest().body(ApiResponse.fail(40001, message, new ErrorData(field, false, null)));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse> handleConflict(DataIntegrityViolationException ex) {
    log.info("data integrity code=40001");
    return ResponseEntity.badRequest()
        .body(ApiResponse.fail(40001, "数据冲突", new ErrorData("body", false, null)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse> handleUnknown(Exception ex) {
    log.error("unhandled {}", ex.getClass().getName(), ex);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.fail(50000, "系统错误", new ErrorData(null, false, null)));
  }
}
