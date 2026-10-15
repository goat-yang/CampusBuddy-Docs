package com.shu.campusbuddy.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Smoke only. Do not put business rules here. Owner A. */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("status", "UP");
    data.put("service", "campusbuddy");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", 0);
    body.put("message", "ok");
    body.put("data", data);
    body.put("requestId", UUID.randomUUID().toString());
    return body;
  }
}
