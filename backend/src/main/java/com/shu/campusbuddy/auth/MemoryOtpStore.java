package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.Hashes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MemoryOtpStore implements OtpStore {

  private final Clock clock;
  private final int ttlSec;
  private final int resendSec;
  private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

  MemoryOtpStore(Clock clock, int ttlSec, int resendSec) {
    this.clock = clock;
    this.ttlSec = ttlSec;
    this.resendSec = resendSec;
  }

  @Override
  public void issue(String campusId, String emailHash, String codeHash) {
    Instant now = clock.instant();
    Entry current = entries.get(campusId);
    if (current != null && now.isBefore(current.nextSendAt())) {
      long wait = Math.max(1, Duration.between(now, current.nextSendAt()).toSeconds());
      throw ApiException.rateLimited(wait);
    }
    entries.put(
        campusId,
        new Entry(emailHash, codeHash, now.plusSeconds(ttlSec), now.plusSeconds(resendSec), 0));
  }

  @Override
  public void verifyAndConsume(String campusId, String emailHash, String codeHash) {
    Entry current = entries.get(campusId);
    if (current == null) {
      throw ApiException.bad("otp", "验证码错误");
    }
    if (!clock.instant().isBefore(current.expiresAt())) {
      entries.remove(campusId);
      throw ApiException.bad("otp", "验证码已过期");
    }
    if (!Hashes.equalHex(current.emailHash(), emailHash) || !Hashes.equalHex(current.codeHash(), codeHash)) {
      int attempts = current.attempts() + 1;
      if (attempts >= 5) {
        entries.remove(campusId);
      } else {
        entries.put(campusId, current.withAttempts(attempts));
      }
      throw ApiException.bad("otp", "验证码错误");
    }
    entries.remove(campusId);
  }

  @Override
  public void clear(String campusId) {
    entries.remove(campusId);
  }

  private record Entry(String emailHash, String codeHash, Instant expiresAt, Instant nextSendAt, int attempts) {
    Entry withAttempts(int next) {
      return new Entry(emailHash, codeHash, expiresAt, nextSendAt, next);
    }
  }
}
