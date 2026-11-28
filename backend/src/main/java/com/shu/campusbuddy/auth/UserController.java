package com.shu.campusbuddy.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.ApiResponse;
import com.shu.campusbuddy.common.IdempotencyService;
import com.shu.campusbuddy.common.Payloads;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private static final Set<String> ME_FIELDS =
      Set.of("nickname", "avatar", "college", "grade", "major", "campus", "bio");

  private final UserService users;
  private final IdempotencyService idempotency;
  private final ObjectMapper mapper;

  public UserController(UserService users, IdempotencyService idempotency, ObjectMapper mapper) {
    this.users = users;
    this.idempotency = idempotency;
    this.mapper = mapper;
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse> me() {
    return noStore(ApiResponse.ok(users.me(AuthContext.require().id())));
  }

  @PutMapping("/me")
  public ResponseEntity<JsonNode> updateMe(
      @RequestHeader("Idempotency-Key") String key, @RequestBody JsonNode body) {
    Payloads.check(body, ME_FIELDS, Set.of(), Set.of("nickname"));
    Requests.UpdateMe request = bind(body, Requests.UpdateMe.class);
    long userId = AuthContext.require().id();
    return noStore(idempotency.execute(userId, key, "/api/v1/users/me", body, () -> ApiResponse.ok(users.update(userId, request))));
  }

  @GetMapping("/me/interests")
  public ResponseEntity<ApiResponse> interests() {
    return noStore(ApiResponse.ok(users.interests(AuthContext.require().id())));
  }

  @PutMapping("/me/interests")
  public ResponseEntity<JsonNode> updateInterests(
      @RequestHeader("Idempotency-Key") String key, @RequestBody JsonNode body) {
    Payloads.check(body, Set.of("tagIds"), Set.of(), Set.of("tagIds"));
    Requests.UpdateInterests request = bind(body, Requests.UpdateInterests.class);
    long userId = AuthContext.require().id();
    return noStore(
        idempotency.execute(
            userId, key, "/api/v1/users/me/interests", body, () -> ApiResponse.ok(users.updateInterests(userId, request.tagIds))));
  }

  @GetMapping("/{id}/profile")
  public ResponseEntity<ApiResponse> profile(@PathVariable String id) {
    AuthContext.require();
    return noStore(ApiResponse.ok(users.publicProfile(parseId(id))));
  }

  private static long parseId(String id) {
    try {
      long value = Long.parseLong(id);
      if (value <= 0) {
        throw ApiException.bad("id", "id 不合法");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw ApiException.bad("id", "id 不合法");
    }
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

  private static ResponseEntity<JsonNode> noStore(ResponseEntity<JsonNode> body) {
    return ResponseEntity.status(body.getStatusCode()).cacheControl(CacheControl.noStore()).body(body.getBody());
  }
}
