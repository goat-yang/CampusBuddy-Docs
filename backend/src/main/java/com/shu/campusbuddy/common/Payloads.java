package com.shu.campusbuddy.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Payloads {

  private Payloads() {}

  public static void check(JsonNode node, Set<String> required, Set<String> optional, Set<String> nonNull) {
    if (node == null || !node.isObject()) {
      throw ApiException.bad("body", "请求体必须是 JSON 对象");
    }
    Set<String> names = new LinkedHashSet<>();
    node.fieldNames().forEachRemaining(names::add);
    for (String name : names) {
      if (!required.contains(name) && !optional.contains(name)) {
        throw ApiException.bad(name, name + " 不允许");
      }
    }
    for (String name : required) {
      if (!node.has(name)) {
        throw ApiException.bad(name, "缺少 " + name);
      }
    }
    for (String name : nonNull) {
      JsonNode value = node.get(name);
      if (value == null || value.isNull()) {
        throw ApiException.bad(name, "缺少 " + name);
      }
    }
  }
}
