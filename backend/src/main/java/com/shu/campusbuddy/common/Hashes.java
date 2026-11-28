package com.shu.campusbuddy.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Hashes {

  private Hashes() {}

  public static String shortHash(String raw) {
    byte[] digest = sha256(raw.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest, 0, 6);
  }

  public static String sha256Hex(String raw) {
    return HexFormat.of().formatHex(sha256(raw.getBytes(StandardCharsets.UTF_8)));
  }

  public static String hmacSha256(String secret, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("hmac unavailable");
    }
  }

  public static boolean equalHex(String left, String right) {
    if (left == null || right == null) {
      return false;
    }
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }

  private static byte[] sha256(byte[] raw) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(raw);
    } catch (Exception ex) {
      throw new IllegalStateException("sha256 unavailable");
    }
  }
}
