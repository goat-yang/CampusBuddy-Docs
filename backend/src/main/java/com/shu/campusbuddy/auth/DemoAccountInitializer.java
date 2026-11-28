package com.shu.campusbuddy.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DemoAccountInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoAccountInitializer.class);

  private final CampusBuddyProperties properties;
  private final UserRepository users;
  private final PasswordHasher passwords;

  public DemoAccountInitializer(CampusBuddyProperties properties, UserRepository users, PasswordHasher passwords) {
    this.properties = properties;
    this.users = users;
    this.passwords = passwords;
  }

  @Override
  public void run(ApplicationArguments args) {
    String campusId = properties.getDemo().getCampusId();
    String password = properties.getDemo().getPassword();
    if (campusId == null || campusId.isBlank() || password == null || password.isBlank()) {
      return;
    }
    if (!campusId.matches("^[A-Za-z0-9]{5,20}$") || password.length() < 8 || password.length() > 64) {
      log.info("demo account skipped");
      return;
    }
    if (users.findByCampusIdAndDeletedFalse(campusId).isPresent()) {
      log.info("demo account already present affiliation=DEMO");
      return;
    }
    UserAccount user = new UserAccount();
    user.setCampusId(campusId);
    user.setPasswordHash(passwords.hash(password));
    user.setNickname("演示同学");
    user.setRole("USER");
    user.setAffiliation("DEMO");
    user.setCreditScore(100);
    user.setAccountStatus("NORMAL");
    users.save(user);
    log.info("demo account ready affiliation=DEMO");
  }
}
