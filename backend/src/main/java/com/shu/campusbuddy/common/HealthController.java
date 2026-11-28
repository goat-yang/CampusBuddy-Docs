package com.shu.campusbuddy.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Smoke only. Do not put business rules here. Owner A. */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

  @GetMapping("/health")
  public ApiResponse health() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("status", "UP");
    data.put("service", "campusbuddy");
    return ApiResponse.ok(data);
  }
}
