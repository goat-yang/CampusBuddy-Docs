package com.shu.campusbuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.shu.campusbuddy.auth.CampusBuddyProperties;

@SpringBootApplication
@EnableConfigurationProperties(CampusBuddyProperties.class)
public class CampusBuddyApplication {
  public static void main(String[] args) {
    SpringApplication.run(CampusBuddyApplication.class, args);
  }
}
