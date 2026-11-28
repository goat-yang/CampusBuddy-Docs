package com.shu.campusbuddy.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

  private static final Pattern UUID =
      Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final IdempotencyRepository records;
  private final ObjectMapper mapper;

  public IdempotencyService(IdempotencyRepository records, ObjectMapper mapper) {
    this.records = records;
    this.mapper = mapper;
  }

  @Transactional
  public ResponseEntity<JsonNode> execute(
      long userId, String key, String path, JsonNode body, Supplier<ApiResponse> action) {
    if (key == null || !UUID.matcher(key).matches()) {
      throw ApiException.bad("Idempotency-Key", "Idempotency-Key 必须是 UUID");
    }
    String hash = CanonicalJson.sha256(body);
    IdempotencyRecord existing = records.findByUserIdAndPathAndIdemKey(userId, path, key).orElse(null);
    if (existing != null) {
      if (!existing.getBodyHash().equals(hash)) {
        throw ApiException.conflict(40905, "幂等键与已提交内容不一致", "Idempotency-Key");
      }
      try {
        return ResponseEntity.status(existing.getHttpStatus()).body(mapper.readTree(existing.getResponseBody()));
      } catch (Exception ex) {
        throw ApiException.system("系统错误");
      }
    }
    ApiResponse response = action.get();
    JsonNode tree = mapper.valueToTree(response);
    IdempotencyRecord row = new IdempotencyRecord();
    row.setUserId(userId);
    row.setIdemKey(key);
    row.setPath(path);
    row.setBodyHash(hash);
    row.setHttpStatus(200);
    row.setResponseBody(tree.toString());
    records.save(row);
    return ResponseEntity.ok(tree);
  }
}
