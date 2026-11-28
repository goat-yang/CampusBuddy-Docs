package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiResponse;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
@RequestMapping("/api/v1/internal-test")
class TestGuardController {

  @GetMapping("/admin")
  @RequireRole("ADMIN")
  ApiResponse admin() {
    return ApiResponse.ok(Map.of("ok", true));
  }

  @PostMapping("/publish")
  @RequireCampusBound
  ApiResponse publish() {
    return ApiResponse.ok(Map.of("ok", true));
  }
}
