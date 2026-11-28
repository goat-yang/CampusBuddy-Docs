package com.shu.campusbuddy.auth;

import java.time.Clock;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthInfrastructure implements WebMvcConfigurer {

  private final AccessInterceptor access;
  private final CampusBuddyProperties properties;

  public AuthInfrastructure(AccessInterceptor access, CampusBuddyProperties properties) {
    this.access = access;
    this.properties = properties;
  }

  @Bean
  @Profile("!test")
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  OtpStore otpStore(Clock clock) {
    String host = properties.getOtp().getRedisHost();
    if (host != null && !host.isBlank()) {
      return new RedisOtpStore(clock, 300, 60, host.trim(), properties.getOtp().getRedisPort());
    }
    return new MemoryOtpStore(clock, 300, 60);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(access).addPathPatterns("/api/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String raw = properties.getCorsOrigins();
    String[] origins =
        raw == null || raw.isBlank()
            ? new String[0]
            : Arrays.stream(raw.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toArray(String[]::new);
    if (origins.length == 0) {
      return;
    }
    registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("*").allowedHeaders("*");
  }
}
