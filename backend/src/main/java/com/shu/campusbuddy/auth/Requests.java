package com.shu.campusbuddy.auth;

import java.util.List;

final class Requests {

  private Requests() {}

  static final class OtpSend {
    public String channel;
    public String campusId;
    public String email;
  }

  static final class Login {
    public String mode;
    public String campusId;
    public String password;
    public String email;
    public String otp;
    public String code;
  }

  static final class Refresh {
    public String refreshToken;
  }

  static final class UpdateMe {
    public String nickname;
    public String avatar;
    public String college;
    public String grade;
    public String major;
    public String campus;
    public String bio;
  }

  static final class UpdateInterests {
    public List<Long> tagIds;
  }
}
