package com.shu.campusbuddy.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

  public String hash(String raw) {
    return encoder.encode(raw);
  }

  public boolean matches(String raw, String hash) {
    if (raw == null || hash == null || hash.isBlank()) {
      return false;
    }
    return encoder.matches(raw, hash);
  }
}
