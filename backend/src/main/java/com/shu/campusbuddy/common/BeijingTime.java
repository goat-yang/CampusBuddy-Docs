package com.shu.campusbuddy.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class BeijingTime {

  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  private BeijingTime() {}

  public static LocalDateTime ofInstant(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZONE);
  }
}
