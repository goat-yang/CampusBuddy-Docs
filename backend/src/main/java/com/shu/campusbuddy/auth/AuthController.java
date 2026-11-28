package com.shu.campusbuddy.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.ApiResponse;
import com.shu.campusbuddy.common.Payloads;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final Set<String> OTP_FIELDS = Set.of("channel", "campusId", "email");
  private static final Set<String> LOGIN_OPTIONAL = Set.of("campusId", "password", "email", "otp", "code");
  private static final Set<String> REFRESH_FIELDS = Set.of("refreshToken");

  private final AuthService auth;
  private final OtpService otp;
  private final ObjectMapper mapper;

  public AuthController(AuthService auth, OtpService otp, ObjectMapper mapper) {
    this.auth = auth;
    this.otp = otp;
    this.mapper = mapper;
  }

  @PostMapping("/otp/send")
  public ResponseEntity<ApiResponse> sendOtp(@RequestBody JsonNode body) {
    Payloads.check(body, OTP_FIELDS, Set.of(), OTP_FIELDS);
    Requests.OtpSend request = bind(body, Requests.OtpSend.class);
    int ttl = otp.send(request.channel, request.campusId, request.email);
    return noStore(ApiResponse.ok(new Views.OtpSent(ttl)));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse> login(@RequestBody JsonNode body) {
    Payloads.check(body, Set.of("mode"), LOGIN_OPTIONAL, Set.of("mode"));
    Requests.Login request = bind(body, Requests.Login.class);
    return noStore(ApiResponse.ok(auth.login(request)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse> refresh(@RequestBody JsonNode body) {
    Payloads.check(body, REFRESH_FIELDS, Set.of(), REFRESH_FIELDS);
    Requests.Refresh request = bind(body, Requests.Refresh.class);
    return noStore(ApiResponse.ok(auth.refresh(request.refreshToken)));
  }

  private <T> T bind(JsonNode body, Class<T> type) {
    try {
      return mapper.convertValue(body, type);
    } catch (IllegalArgumentException ex) {
      throw ApiException.bad("body", "请求体无法解析");
    }
  }

  private static ResponseEntity<ApiResponse> noStore(ApiResponse body) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
  }
}
