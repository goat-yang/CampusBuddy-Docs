package com.shu.campusbuddy.auth;

import java.util.List;

final class Views {

  private Views() {}

  record UserBriefView(
      long id, String nickname, String avatar, int creditScore, String creditLevel, String college) {}

  record TokenBundle(String accessToken, String refreshToken, int expiresIn, UserBriefView user) {}

  record MeView(
      long id,
      String nickname,
      String avatar,
      int creditScore,
      String creditLevel,
      String college,
      String campusIdMasked,
      String grade,
      String major,
      String campus,
      String bio,
      String emailMasked) {}

  record ReviewSummary(double avgScore, int count) {}

  record PublicProfileView(
      long id,
      String nickname,
      String avatar,
      int creditScore,
      String creditLevel,
      String college,
      int historyCount,
      ReviewSummary reviewSummary) {}

  record TagView(long id, String name, String category) {}

  record InterestsView(List<Long> tagIds, List<TagView> tags) {}

  record OtpSent(int ttlSec) {}

  static UserBriefView brief(UserAccount user) {
    int score = Math.max(0, Math.min(100, user.getCreditScore()));
    return new UserBriefView(
        user.getId(), user.getNickname(), user.getAvatar(), score, CreditLevels.of(score), user.getCollege());
  }

  static MeView me(UserAccount user) {
    UserBriefView brief = brief(user);
    return new MeView(
        brief.id(),
        brief.nickname(),
        brief.avatar(),
        brief.creditScore(),
        brief.creditLevel(),
        brief.college(),
        Masks.campusId(user.getCampusId()),
        user.getGrade(),
        user.getMajor(),
        user.getCampus(),
        user.getBio(),
        Masks.email(user.getSchoolEmail()));
  }
}
