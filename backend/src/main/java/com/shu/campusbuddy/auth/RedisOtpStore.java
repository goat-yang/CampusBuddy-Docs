package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.Hashes;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

final class RedisOtpStore implements OtpStore, DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(RedisOtpStore.class);

  private final Clock clock;
  private final int ttlSec;
  private final int resendSec;
  private final String host;
  private final int port;
  private RedisClient client;
  private StatefulRedisConnection<String, String> connection;

  RedisOtpStore(Clock clock, int ttlSec, int resendSec, String host, int port) {
    this.clock = clock;
    this.ttlSec = ttlSec;
    this.resendSec = resendSec;
    this.host = host;
    this.port = port;
  }

  @Override
  public void issue(String campusId, String emailHash, String codeHash) {
    RedisCommands<String, String> commands = commands();
    String key = key(campusId);
    long now = clock.millis();
    String next = commands.hget(key, "next");
    if (next != null && now < Long.parseLong(next)) {
      long wait = Math.max(1, (Long.parseLong(next) - now + 999) / 1000);
      throw ApiException.rateLimited(wait);
    }
    commands.hset(
        key,
        Map.of(
            "email",
            emailHash,
            "code",
            codeHash,
            "exp",
            Long.toString(now + ttlSec * 1000L),
            "next",
            Long.toString(now + resendSec * 1000L),
            "tries",
            "0"));
    commands.expire(key, Duration.ofSeconds(ttlSec));
  }

  @Override
  public void verifyAndConsume(String campusId, String emailHash, String codeHash) {
    RedisCommands<String, String> commands = commands();
    String key = key(campusId);
    Map<String, String> row = commands.hgetall(key);
    if (row == null || row.isEmpty()) {
      throw ApiException.bad("otp", "验证码错误");
    }
    long now = clock.millis();
    if (now >= Long.parseLong(row.get("exp"))) {
      commands.del(key);
      throw ApiException.bad("otp", "验证码已过期");
    }
    if (!Hashes.equalHex(row.get("email"), emailHash) || !Hashes.equalHex(row.get("code"), codeHash)) {
      int tries = Integer.parseInt(row.getOrDefault("tries", "0")) + 1;
      if (tries >= 5) {
        commands.del(key);
      } else {
        commands.hset(key, "tries", Integer.toString(tries));
      }
      throw ApiException.bad("otp", "验证码错误");
    }
    commands.del(key);
  }

  @Override
  public void clear(String campusId) {
    commands().del(key(campusId));
  }

  private RedisCommands<String, String> commands() {
    if (connection != null && connection.isOpen()) {
      return connection.sync();
    }
    try {
      client = RedisClient.create("redis://" + host + ":" + port);
      connection = client.connect();
      return connection.sync();
    } catch (RuntimeException ex) {
      log.info("otp redis unavailable");
      throw ApiException.system("系统错误");
    }
  }

  private static String key(String campusId) {
    return "cb:a:otp:" + Hashes.sha256Hex(campusId);
  }

  @Override
  public void destroy() {
    if (connection != null) {
      connection.close();
    }
    if (client != null) {
      client.shutdown();
    }
  }
}
