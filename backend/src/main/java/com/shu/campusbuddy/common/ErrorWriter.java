package com.shu.campusbuddy.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class ErrorWriter {

  private final ObjectMapper mapper;

  public ErrorWriter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public void write(HttpServletResponse response, ApiException ex) throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(ex.getHttpStatus());
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;charset=UTF-8");
    Object data = ex.getHttpStatus() == 404 ? null : new ErrorData(ex.getField(), ex.isRetryable(), ex.getRetryAfterSec());
    mapper.writeValue(response.getOutputStream(), ApiResponse.fail(ex.getCode(), ex.getMessage(), data));
  }
}
