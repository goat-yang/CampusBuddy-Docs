package com.shu.campusbuddy.auth;

import java.time.Clock;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
class TestClockConfig {

  @Bean
  Clock clock() {
    return new MutableClock(Instant.parse("2026-11-28T02:00:00Z"));
  }
}
