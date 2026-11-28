package com.shu.campusbuddy.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CanonicalJson {

  private CanonicalJson() {}

  public static String sha256(JsonNode node) {
    return Hashes.sha256Hex(write(node));
  }

  private static String write(JsonNode node) {
    if (node == null || node.isNull()) {
      return "null";
    }
    if (node.isObject()) {
      List<String> names = new ArrayList<>();
      node.fieldNames().forEachRemaining(names::add);
      Collections.sort(names);
      StringBuilder builder = new StringBuilder("{");
      for (int i = 0; i < names.size(); i++) {
        if (i > 0) {
          builder.append(',');
        }
        String name = names.get(i);
        builder.append(name).append(':').append(write(node.get(name)));
      }
      return builder.append('}').toString();
    }
    if (node.isArray()) {
      StringBuilder builder = new StringBuilder("[");
      for (int i = 0; i < node.size(); i++) {
        if (i > 0) {
          builder.append(',');
        }
        builder.append(write(node.get(i)));
      }
      return builder.append(']').toString();
    }
    return node.toString();
  }
}
