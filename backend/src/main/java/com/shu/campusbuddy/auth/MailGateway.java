package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailGateway {

  private static final Logger log = LoggerFactory.getLogger(MailGateway.class);

  private final JavaMailSender sender;
  private final String from;
  private final boolean enabled;

  public MailGateway(ObjectProvider<JavaMailSender> senders, CampusBuddyProperties properties) {
    this.sender = senders.getIfAvailable();
    String username = properties.getMail().getFrom();
    this.enabled = sender != null && username != null && username.contains("@") && !username.startsWith("noreply@");
    this.from = username == null || username.isBlank() ? "noreply@localhost" : username;
  }

  public void sendOtp(String to, String code) {
    if (!enabled) {
      log.info("smtp skipped domain={}", EmailDomainPolicy.domain(to));
      throw ApiException.system("邮件服务未配置");
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject("校园搭子验证码");
    message.setText("你的校园搭子验证码是 " + code + " ，5 分钟内有效。这是平台自己的验证码，不是学校统一认证密码。");
    try {
      sender.send(message);
    } catch (RuntimeException ex) {
      log.info("smtp failed domain={}", EmailDomainPolicy.domain(to));
      throw ApiException.system("系统错误");
    }
    log.info("smtp accepted domain={}", EmailDomainPolicy.domain(to));
  }
}
