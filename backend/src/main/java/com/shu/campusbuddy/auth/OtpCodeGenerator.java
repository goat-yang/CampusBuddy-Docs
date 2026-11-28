package com.shu.campusbuddy.auth;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

public interface OtpCodeGenerator {
  String nextCode();
}

@Component
class SecureOtpCodeGenerator implements OtpCodeGenerator {

  private final SecureRandom random = new SecureRandom();

  @Override
  public String nextCode() {
    return String.format("%06d", random.nextInt(1_000_000));
  }
}
