package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.Hashes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

  private static final Logger log = LoggerFactory.getLogger(OtpService.class);

  private final OtpStore store;
  private final OtpCodeGenerator generator;
  private final MailGateway mail;
  private final JwtService jwt;

  public OtpService(OtpStore store, OtpCodeGenerator generator, MailGateway mail, JwtService jwt) {
    this.store = store;
    this.generator = generator;
    this.mail = mail;
    this.jwt = jwt;
  }

  public int send(String channel, String campusId, String email) {
    if ("SMS".equals(channel)) {
      throw ApiException.bad("channel", "本期仅支持学校邮箱验证码");
    }
    if (!"EMAIL".equals(channel)) {
      throw ApiException.bad("channel", "channel 不合法");
    }
    String campus = requireCampus(campusId);
    String normalized = requireSchoolEmail(email);
    String code = generator.nextCode();
    if (code == null || !code.matches("\\d{6}")) {
      throw ApiException.system("系统错误");
    }
    store.issue(campus, Hashes.hmacSha256(jwt.pepper(), normalized), Hashes.hmacSha256(jwt.pepper(), code));
    try {
      mail.sendOtp(normalized, code);
    } catch (RuntimeException ex) {
      store.clear(campus);
      if (ex instanceof ApiException api) {
        throw api;
      }
      throw ApiException.system("系统错误");
    }
    log.info(
        "otp accepted domain={} campusHash={}",
        EmailDomainPolicy.domain(normalized),
        Hashes.shortHash(campus));
    return 300;
  }

  public void verify(String campusId, String email, String otp) {
    String campus = requireCampus(campusId);
    String normalized = requireSchoolEmail(email);
    if (otp == null || !otp.matches("\\d{6}")) {
      throw ApiException.bad("otp", "验证码错误");
    }
    store.verifyAndConsume(
        campus, Hashes.hmacSha256(jwt.pepper(), normalized), Hashes.hmacSha256(jwt.pepper(), otp));
  }

  private static String requireCampus(String campusId) {
    if (campusId == null || !campusId.trim().matches("^[A-Za-z0-9]{5,20}$")) {
      throw ApiException.bad("campusId", "campusId 格式不正确");
    }
    return campusId.trim();
  }

  private static String requireSchoolEmail(String email) {
    if (email == null || email.isBlank()) {
      throw ApiException.bad("email", "email 必须是学校邮箱域");
    }
    String normalized = EmailDomainPolicy.normalize(email);
    if (!EmailDomainPolicy.allowed(normalized)) {
      throw ApiException.bad("email", "email 必须是学校邮箱域");
    }
    return normalized;
  }
}
