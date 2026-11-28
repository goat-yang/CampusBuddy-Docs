package com.shu.campusbuddy.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import com.shu.campusbuddy.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestClockConfig.class, TestGuardController.class})
class AuthApiTest {

  private static final AtomicInteger SEQ = new AtomicInteger(10);
  private static final String PASSWORD = "PlatformPass1";
  private static final String OTP = "847291";

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;
  @Autowired PasswordHasher passwords;
  @Autowired JwtService jwt;
  @Autowired Clock clock;

  @MockitoBean OtpCodeGenerator codes;
  @MockitoBean MailGateway mail;
  @MockitoBean WechatIdentityClient wechat;

  @BeforeEach
  void stubs() {
    lenient().when(codes.nextCode()).thenReturn(OTP);
    lenient().when(wechat.openIdForCode("wx-ok")).thenReturn("openid-wx-1");
    lenient().when(wechat.openIdForCode("wx-bind")).thenReturn("openid-wx-bind");
    lenient().when(wechat.openIdForCode("wx-2")).thenReturn("openid-wx-2");
    lenient().when(wechat.openIdForCode("wx-bad")).thenThrow(ApiException.bad("code", "微信登录码无效"));
  }

  @Test
  void healthIsOpen() throws Exception {
    mockMvc.perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("UP"))
        .andExpect(jsonPath("$.data.service").value("campusbuddy"));
  }

  @Test
  void rejectsNonShuEmailSmsAndResend() throws Exception {
    String campus = nextCampus();
    mockMvc.perform(post("/api/v1/auth/otp/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpBody(campus, "student@gmail.com")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001))
        .andExpect(jsonPath("$.message", containsString("学校邮箱")));

    mockMvc.perform(post("/api/v1/auth/otp/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"SMS\",\"campusId\":\"" + campus + "\",\"email\":\"a@shu.edu.cn\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001))
        .andExpect(jsonPath("$.message", containsString("学校邮箱验证码")));

    String email = campus + "@shu.edu.cn";
    mockMvc.perform(post("/api/v1/auth/otp/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpBody(campus, email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.ttlSec").value(300));
    mockMvc.perform(post("/api/v1/auth/otp/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpBody(campus, email)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(42900))
        .andExpect(jsonPath("$.data.retryable").value(true));

    ((MutableClock) clock).advance(Duration.ofSeconds(61));
    mockMvc.perform(post("/api/v1/auth/otp/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpBody(campus, email)))
        .andExpect(status().isOk());
  }

  @Test
  void otpDoesNotAppearInLogs() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger("com.shu.campusbuddy");
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    try {
      mockMvc.perform(post("/api/v1/auth/otp/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(otpBody(campus, email)))
          .andExpect(status().isOk());
      verify(mail).sendOtp(anyString(), anyString());
      for (ILoggingEvent event : appender.list) {
        assertFalse(event.getFormattedMessage().contains(OTP), event.getFormattedMessage());
        assertFalse(event.getFormattedMessage().contains(email), event.getFormattedMessage());
        assertFalse(event.getFormattedMessage().contains(PASSWORD));
      }
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void firstOtpSetsBcryptPasswordAndSecondClientIsSameUser() throws Exception {
    String campus = "24123456";
    String email = "24123456@SHU.EDU.CN";
    sendOtp(campus, email);
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpLogin(campus, email, OTP, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001))
        .andExpect(jsonPath("$.message", containsString("本平台密码")));

    ((MutableClock) clock).advance(Duration.ofSeconds(61));
    sendOtp(campus, email);
    MvcResult created = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpLogin(campus, email, OTP, PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.expiresIn").value(7200))
        .andExpect(jsonPath("$.data.user.creditLevel").value("EXCELLENT"))
        .andReturn();
    int userId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.user.id");
    UserAccount stored = users.findByCampusIdAndDeletedFalse(campus).orElseThrow();
    assertTrue(stored.getPasswordHash().startsWith("$2"));
    assertFalse(PASSWORD.equals(stored.getPasswordHash()));
    assertTrue(passwords.matches(PASSWORD, stored.getPasswordHash()));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"CAMPUS_PASSWORD\",\"campusId\":\"" + campus + "\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(40100));

    MvcResult again = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"CAMPUS_PASSWORD\",\"campusId\":\"" + campus + "\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    int second = JsonPath.read(again.getResponse().getContentAsString(), "$.data.user.id");
    assertTrue(userId == second);

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"CAMPUS_PASSWORD\",\"campusId\":\"" + campus + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001));
    assertTrue("USER".equals(users.findByCampusIdAndDeletedFalse(campus).orElseThrow().getRole()));
  }

  @Test
  void wrongOtpAndExpiredCodeAreRejected() throws Exception {
    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    sendOtp(campus, email);
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpLogin(campus, email, "000000", PASSWORD)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001));
    ((MutableClock) clock).advance(Duration.ofSeconds(301));
    sendOtp(campus, email);
    ((MutableClock) clock).advance(Duration.ofSeconds(301));
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpLogin(campus, email, OTP, PASSWORD)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("过期")));
  }

  @Test
  void refreshReplacesExpiredAccessAndRejectsReuse() throws Exception {
    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    sendOtp(campus, email);
    MvcResult login = loginOtp(campus, email);
    String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");
    int userId = JsonPath.read(login.getResponse().getContentAsString(), "$.data.user.id");
    String expired = jwt.sign(userId, "USER", "access", null, Instant.now().minusSeconds(30));
    mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(40100));
    mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + refresh))
        .andExpect(status().isUnauthorized());

    MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.id").value(userId))
        .andReturn();
    String access = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.data.accessToken");
    mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.campusIdMasked", startsWith(campus.substring(0, 4))))
        .andExpect(jsonPath("$.data.emailMasked", containsString("@shu.edu.cn")))
        .andExpect(jsonPath("$.data.campusId").doesNotExist())
        .andExpect(jsonPath("$.data.email").doesNotExist());
    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(40100));
  }

  @Test
  void profileIsMaskedAndMissingUserIs404() throws Exception {
    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    sendOtp(campus, email);
    MvcResult login = loginOtp(campus, email);
    String access = JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
    int userId = JsonPath.read(login.getResponse().getContentAsString(), "$.data.user.id");
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/users/" + userId + "/profile").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.historyCount").value(0))
        .andExpect(jsonPath("$.data.reviewSummary.count").value(0))
        .andExpect(jsonPath("$.data.campusId").doesNotExist())
        .andExpect(jsonPath("$.data.email").doesNotExist())
        .andExpect(jsonPath("$.data.phone").doesNotExist())
        .andExpect(jsonPath("$.data.openid").doesNotExist());
    mockMvc.perform(get("/api/v1/users/999999/profile").header("Authorization", "Bearer " + access))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(40400));
  }

  @Test
  void interestsAndIdempotentProfileUpdate() throws Exception {
    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    sendOtp(campus, email);
    String access = JsonPath.read(loginOtp(campus, email).getResponse().getContentAsString(), "$.data.accessToken");
    String key = UUID.randomUUID().toString();
    String body = meBody("新昵称", "你好");
    MvcResult first = mockMvc.perform(put("/api/v1/users/me")
            .header("Authorization", "Bearer " + access)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("新昵称"))
        .andExpect(jsonPath("$.data.campus").value("BAOSHAN"))
        .andReturn();
    String requestId = JsonPath.read(first.getResponse().getContentAsString(), "$.requestId");
    mockMvc.perform(put("/api/v1/users/me")
            .header("Authorization", "Bearer " + access)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value(requestId));
    mockMvc.perform(put("/api/v1/users/me")
            .header("Authorization", "Bearer " + access)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(meBody("新昵称", "另一份简介")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(40905));
    mockMvc.perform(put("/api/v1/users/me")
            .header("Authorization", "Bearer " + access)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001));

    mockMvc.perform(put("/api/v1/users/me/interests")
            .header("Authorization", "Bearer " + access)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tagIds\":[1,2]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001));
    mockMvc.perform(put("/api/v1/users/me/interests")
            .header("Authorization", "Bearer " + access)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tagIds\":[1,2,3]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tagIds.length()").value(3))
        .andExpect(jsonPath("$.data.tags[0].name").value("拼饭"));
  }

  @Test
  void wechatBindJoinsTheCampusUser() throws Exception {
    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    sendOtp(campus, email);
    int campusUser = JsonPath.read(loginOtp(campus, email).getResponse().getContentAsString(), "$.data.user.id");
    MvcResult wechat = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"WECHAT\",\"code\":\"wx-bind\"}"))
        .andExpect(status().isOk())
        .andReturn();
    int shell = JsonPath.read(wechat.getResponse().getContentAsString(), "$.data.user.id");
    String shellToken = JsonPath.read(wechat.getResponse().getContentAsString(), "$.data.accessToken");
    assertFalse(campusUser == shell);
    mockMvc.perform(post("/api/v1/internal-test/publish").header("Authorization", "Bearer " + shellToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40300))
        .andExpect(jsonPath("$.message", containsString("先绑定学号")));

    ((MutableClock) clock).advance(Duration.ofSeconds(61));
    sendOtp(campus, email);
    MvcResult bound = mockMvc.perform(post("/api/v1/auth/login")
            .header("Authorization", "Bearer " + shellToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpLogin(campus, email, OTP, null)))
        .andExpect(status().isOk())
        .andReturn();
    int boundId = JsonPath.read(bound.getResponse().getContentAsString(), "$.data.user.id");
    assertTrue(boundId == campusUser);
    MvcResult again = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"WECHAT\",\"code\":\"wx-bind\"}"))
        .andExpect(status().isOk())
        .andReturn();
    int wechatAgain = JsonPath.read(again.getResponse().getContentAsString(), "$.data.user.id");
    assertTrue(wechatAgain == campusUser);
  }

  @Test
  void adminRoleComesFromJwt() throws Exception {
    UserAccount admin = new UserAccount();
    admin.setCampusId("admin01");
    admin.setPasswordHash(passwords.hash(PASSWORD));
    admin.setNickname("管理员");
    admin.setRole("ADMIN");
    admin.setAffiliation("DEMO");
    admin.setCreditScore(100);
    admin.setAccountStatus("NORMAL");
    users.save(admin);
    MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"CAMPUS_PASSWORD\",\"campusId\":\"admin01\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String access = JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
    assertTrue("ADMIN".equals(jwt.parse(access).role()));
    mockMvc.perform(get("/api/v1/internal-test/admin").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk());

    String campus = nextCampus();
    String email = campus + "@shu.edu.cn";
    sendOtp(campus, email);
    String userToken = JsonPath.read(loginOtp(campus, email).getResponse().getContentAsString(), "$.data.accessToken");
    assertTrue("USER".equals(jwt.parse(userToken).role()));
    mockMvc.perform(get("/api/v1/internal-test/admin").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40300))
        .andExpect(jsonPath("$.message", containsString("无权访问")));
  }

  @Test
  void twoUnboundWechatUsersCanBothBrowse() throws Exception {
    MvcResult first = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"WECHAT\",\"code\":\"wx-ok\"}"))
        .andExpect(status().isOk())
        .andReturn();
    ((MutableClock) clock).advance(Duration.ofSeconds(1));
    MvcResult second = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"WECHAT\",\"code\":\"wx-2\"}"))
        .andExpect(status().isOk())
        .andReturn();
    int a = JsonPath.read(first.getResponse().getContentAsString(), "$.data.user.id");
    int b = JsonPath.read(second.getResponse().getContentAsString(), "$.data.user.id");
    assertFalse(a == b);
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"WECHAT\",\"code\":\"wx-bad\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001));
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"SHU_OAUTH\",\"campusId\":\"24120000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40001));
  }

  private MvcResult loginOtp(String campus, String email) throws Exception {
    return mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpLogin(campus, email, OTP, PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken", not(containsString(PASSWORD))))
        .andReturn();
  }

  private void sendOtp(String campus, String email) throws Exception {
    mockMvc.perform(post("/api/v1/auth/otp/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(otpBody(campus, email)))
        .andExpect(status().isOk());
  }

  private static String otpBody(String campus, String email) {
    return "{\"channel\":\"EMAIL\",\"campusId\":\"" + campus + "\",\"email\":\"" + email + "\"}";
  }

  private static String otpLogin(String campus, String email, String otp, String password) {
    String passwordField = password == null ? "" : ",\"password\":\"" + password + "\"";
    return "{\"mode\":\"CAMPUS_OTP\",\"campusId\":\"" + campus + "\",\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"" + passwordField + "}";
  }

  private static String meBody(String nickname, String bio) {
    return "{\"nickname\":\"" + nickname + "\",\"avatar\":null,\"college\":\"计算机学院\",\"grade\":\"2024\",\"major\":\"软件工程\",\"campus\":\"BAOSHAN\",\"bio\":\"" + bio + "\"}";
  }

  private static String nextCampus() {
    return "24" + String.format("%06d", SEQ.incrementAndGet());
  }
}
