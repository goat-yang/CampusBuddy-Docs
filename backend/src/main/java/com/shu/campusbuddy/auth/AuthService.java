package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.BeijingTime;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordHasher passwords;
  private final OtpService otp;
  private final JwtService jwt;
  private final WechatIdentityClient wechat;
  private final CampusBuddyProperties properties;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordHasher passwords,
      OtpService otp,
      JwtService jwt,
      WechatIdentityClient wechat,
      CampusBuddyProperties properties) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwords = passwords;
    this.otp = otp;
    this.jwt = jwt;
    this.wechat = wechat;
    this.properties = properties;
  }

  @Transactional
  public Views.TokenBundle login(Requests.Login request) {
    String mode = request.mode == null ? "" : request.mode.trim();
    return switch (mode) {
      case "CAMPUS_PASSWORD" -> loginPassword(request);
      case "CAMPUS_OTP" -> loginOtp(request, AuthContext.find().orElse(null));
      case "WECHAT" -> loginWechat(request);
      default -> throw ApiException.bad("mode", "mode 不合法");
    };
  }

  @Transactional
  public Views.TokenBundle refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw ApiException.bad("refreshToken", "缺少 refreshToken");
    }
    JwtService.TokenClaims claims = jwt.parse(refreshToken);
    if (!"refresh".equals(claims.typ()) || claims.jti() == null) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
    RefreshToken row =
        refreshTokens.findByJti(claims.jti()).orElseThrow(() -> ApiException.unauthorized("未登录或令牌已过期"));
    if (row.isRevoked() || row.getExpiresAt().isBefore(BeijingTime.ofInstant(Instant.now()))) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
    UserAccount user =
        users.findById(claims.userId()).orElseThrow(() -> ApiException.unauthorized("未登录或令牌已过期"));
    if (user.isDeleted()) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
    if ("BANNED_ALL".equals(user.getAccountStatus())) {
      throw ApiException.forbidden("账号已封禁", "account");
    }
    row.setRevoked(true);
    return issue(user);
  }

  private Views.TokenBundle loginPassword(Requests.Login request) {
    String campusId = requiredCampus(request.campusId);
    if (request.password == null || request.password.isBlank()) {
      throw ApiException.bad("password", "缺少 password");
    }
    UserAccount user = users.findByCampusIdAndDeletedFalse(campusId).orElse(null);
    if (user == null || !passwords.matches(request.password, user.getPasswordHash())) {
      throw ApiException.unauthorized("学号或密码错误");
    }
    if ("BANNED_ALL".equals(user.getAccountStatus())) {
      throw ApiException.forbidden("账号已封禁", "account");
    }
    return issue(user);
  }

  private Views.TokenBundle loginOtp(Requests.Login request, CurrentUser bearer) {
    String campusId = requiredCampus(request.campusId);
    if (request.email == null || request.email.isBlank()) {
      throw ApiException.bad("email", "缺少 email");
    }
    String email = EmailDomainPolicy.normalize(request.email);
    if (!EmailDomainPolicy.allowed(email)) {
      throw ApiException.bad("email", "email 必须是学校邮箱域");
    }
    if (request.otp == null || !request.otp.matches("\\d{6}")) {
      throw ApiException.bad("otp", "验证码错误");
    }
    otp.verify(campusId, email, request.otp);
    UserAccount campusUser = users.findByCampusIdAndDeletedFalse(campusId).orElse(null);
    UserAccount bearerUser = bearer == null ? null : users.findById(bearer.id()).orElse(null);
    if (bearerUser != null && bearerUser.isDeleted()) {
      bearerUser = null;
    }
    if (campusUser == null) {
      if (bearerUser != null && !isBlank(bearerUser.getCampusId()) && !campusId.equals(bearerUser.getCampusId())) {
        throw ApiException.bad("campusId", "该微信已绑定其他学号");
      }
      if (bearerUser != null && isBlank(bearerUser.getCampusId())) {
        campusUser = bearerUser;
        campusUser.setCampusId(campusId);
        campusUser.setAffiliation("SHU");
      } else if (bearerUser != null) {
        campusUser = bearerUser;
      } else {
        campusUser = newCampusUser(campusId, email);
      }
    } else if (bearerUser != null && !bearerUser.getId().equals(campusUser.getId())) {
      attachWechat(campusUser, bearerUser);
    }
    if (campusUser.getSchoolEmail() != null && !campusUser.getSchoolEmail().equals(email)) {
      throw ApiException.bad("email", "邮箱与学号不匹配");
    }
    ensureEmailAvailable(email, campusUser.getId());
    campusUser.setSchoolEmail(email);
    if (isBlank(campusUser.getNickname())) {
      campusUser.setNickname(nicknameFor(campusId));
    }
    if (isBlank(campusUser.getRole())) {
      campusUser.setRole("USER");
    }
    ensurePassword(campusUser, request.password);
    users.save(campusUser);
    return issue(users.saveAndFlush(campusUser));
  }

  private Views.TokenBundle loginWechat(Requests.Login request) {
    if (request.code == null || request.code.isBlank()) {
      throw ApiException.bad("code", "缺少 code");
    }
    String openid = wechat.openIdForCode(request.code.trim());
    if (openid == null || openid.isBlank()) {
      throw ApiException.bad("code", "微信登录码无效");
    }
    UserAccount user = users.findByOpenidAndDeletedFalse(openid).orElse(null);
    if (user == null) {
      user = new UserAccount();
      user.setOpenid(openid);
      user.setNickname("微信用户");
      user.setRole("USER");
      user.setCreditScore(100);
      user.setAccountStatus("NORMAL");
      user = users.saveAndFlush(user);
    }
    if ("BANNED_ALL".equals(user.getAccountStatus())) {
      throw ApiException.forbidden("账号已封禁", "account");
    }
    return issue(user);
  }

  private void attachWechat(UserAccount campusUser, UserAccount shell) {
    if (!isBlank(shell.getCampusId())) {
      throw ApiException.bad("campusId", "该微信已绑定其他学号");
    }
    if (!isBlank(campusUser.getOpenid()) && !campusUser.getOpenid().equals(shell.getOpenid())) {
      throw ApiException.bad("code", "该学号已绑定其他微信");
    }
    String openid = shell.getOpenid();
    shell.setOpenid("retired-" + shell.getId());
    shell.setDeleted(true);
    users.saveAndFlush(shell);
    campusUser.setOpenid(openid);
  }

  private UserAccount newCampusUser(String campusId, String email) {
    ensureEmailAvailable(email, null);
    UserAccount user = new UserAccount();
    user.setCampusId(campusId);
    user.setSchoolEmail(email);
    user.setNickname(nicknameFor(campusId));
    user.setRole("USER");
    user.setAffiliation("SHU");
    user.setCreditScore(100);
    user.setAccountStatus("NORMAL");
    return user;
  }

  private void ensureEmailAvailable(String email, Long selfId) {
    users.findBySchoolEmailAndDeletedFalse(email)
        .filter(owner -> selfId == null || !selfId.equals(owner.getId()))
        .ifPresent(owner -> {
          throw ApiException.bad("email", "邮箱已被使用");
        });
  }

  private void ensurePassword(UserAccount user, String password) {
    if (!isBlank(user.getPasswordHash())) {
      return;
    }
    if (password == null || password.isBlank()) {
      throw ApiException.bad("password", "首次登录需要设置本平台密码");
    }
    if (password.length() < 8 || password.length() > 64) {
      throw ApiException.bad("password", "password 需为 8-64 位");
    }
    user.setPasswordHash(passwords.hash(password));
  }

  private Views.TokenBundle issue(UserAccount user) {
    if (user.getId() == null) {
      user = users.saveAndFlush(user);
    }
    int accessTtl = properties.getJwt().getAccessTtlSec();
    String access =
        jwt.sign(user.getId(), user.getRole(), "access", null, Instant.now().plusSeconds(accessTtl));
    String jti = UUID.randomUUID().toString();
    Instant refreshExp = Instant.now().plusSeconds(properties.getJwt().getRefreshTtlSec());
    String refresh = jwt.sign(user.getId(), user.getRole(), "refresh", jti, refreshExp);
    RefreshToken row = new RefreshToken();
    row.setUserId(user.getId());
    row.setJti(jti);
    row.setExpiresAt(BeijingTime.ofInstant(refreshExp));
    refreshTokens.save(row);
    return new Views.TokenBundle(access, refresh, accessTtl, Views.brief(user));
  }

  private static String requiredCampus(String campusId) {
    if (campusId == null || !campusId.trim().matches("^[A-Za-z0-9]{5,20}$")) {
      throw ApiException.bad("campusId", "campusId 格式不正确");
    }
    return campusId.trim();
  }

  private static String nicknameFor(String campusId) {
    String tail = campusId.length() <= 4 ? campusId : campusId.substring(campusId.length() - 4);
    return "同学" + tail;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
