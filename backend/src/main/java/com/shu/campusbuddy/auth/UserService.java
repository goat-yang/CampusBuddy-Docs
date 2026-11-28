package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.BeijingTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private static final Set<String> CAMPUSES = Set.of("BAOSHAN", "YANCHANG", "JIADING", "OTHER");

  private final UserRepository users;
  private final TagRepository tags;
  private final UserTagRepository links;

  public UserService(UserRepository users, TagRepository tags, UserTagRepository links) {
    this.users = users;
    this.tags = tags;
    this.links = links;
  }

  public Views.MeView me(long userId) {
    return Views.me(load(userId));
  }

  @Transactional
  public Views.MeView update(long userId, Requests.UpdateMe request) {
    UserAccount user = load(userId);
    user.setNickname(requiredText(request.nickname, "nickname", 20));
    user.setAvatar(optionalText(request.avatar, "avatar", 512));
    user.setCollege(optionalText(request.college, "college", 64));
    user.setGrade(optionalText(request.grade, "grade", 32));
    user.setMajor(optionalText(request.major, "major", 64));
    user.setCampus(campus(request.campus));
    user.setBio(optionalText(request.bio, "bio", 200));
    return Views.me(users.save(user));
  }

  public Views.InterestsView interests(long userId) {
    load(userId);
    return toInterests(links.findByUserIdOrderBySortNoAsc(userId));
  }

  @Transactional
  public Views.InterestsView updateInterests(long userId, List<Long> tagIds) {
    load(userId);
    if (tagIds == null || tagIds.size() < 3 || tagIds.size() > 10) {
      throw ApiException.bad("tagIds", "兴趣标签需要 3 到 10 个");
    }
    if (tagIds.stream().anyMatch(id -> id == null || id <= 0) || new HashSet<>(tagIds).size() != tagIds.size()) {
      throw ApiException.bad("tagIds", "兴趣标签不能重复");
    }
    List<InterestTag> found = tags.findByIdInAndDeletedFalse(tagIds);
    if (found.size() != tagIds.size()) {
      throw ApiException.bad("tagIds", "包含不存在的兴趣标签");
    }
    links.deleteByUserId(userId);
    int sort = 0;
    for (Long tagId : tagIds) {
      UserTagLink link = new UserTagLink();
      link.setUserId(userId);
      link.setTagId(tagId);
      link.setSortNo(sort++);
      link.setCreateTime(BeijingTime.ofInstant(java.time.Instant.now()));
      links.save(link);
    }
    return toInterests(links.findByUserIdOrderBySortNoAsc(userId));
  }

  /**
   * History and reviews stay at zero until modules B and D own those tables.
   * The payload never includes campus id, email, phone, or openid.
   */
  public Views.PublicProfileView publicProfile(long userId) {
    UserAccount user = users.findById(userId).filter(row -> !row.isDeleted()).orElseThrow(ApiException::notFound);
    Views.UserBriefView brief = Views.brief(user);
    return new Views.PublicProfileView(
        brief.id(),
        brief.nickname(),
        brief.avatar(),
        brief.creditScore(),
        brief.creditLevel(),
        brief.college(),
        0,
        new Views.ReviewSummary(0.0, 0));
  }

  private Views.InterestsView toInterests(List<UserTagLink> rows) {
    if (rows.isEmpty()) {
      return new Views.InterestsView(List.of(), List.of());
    }
    List<Long> ids = rows.stream().map(UserTagLink::getTagId).toList();
    Map<Long, InterestTag> byId =
        tags.findByIdInAndDeletedFalse(ids).stream()
            .collect(Collectors.toMap(InterestTag::getId, Function.identity()));
    List<Views.TagView> views = new ArrayList<>();
    for (Long id : ids) {
      InterestTag tag = byId.get(id);
      if (tag != null) {
        views.add(new Views.TagView(tag.getId(), tag.getName(), tag.getCategory()));
      }
    }
    return new Views.InterestsView(ids, views);
  }

  private UserAccount load(long userId) {
    return users.findById(userId).filter(row -> !row.isDeleted()).orElseThrow(ApiException::notFound);
  }

  private static String requiredText(String value, String field, int max) {
    if (value == null || value.trim().isEmpty()) {
      throw ApiException.bad(field, "缺少 " + field);
    }
    String trimmed = value.trim();
    if (trimmed.codePointCount(0, trimmed.length()) > max) {
      throw ApiException.bad(field, field + " 过长");
    }
    return trimmed;
  }

  private static String optionalText(String value, String field, int max) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.codePointCount(0, trimmed.length()) > max) {
      throw ApiException.bad(field, field + " 过长");
    }
    return trimmed;
  }

  private static String campus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    if (!CAMPUSES.contains(value)) {
      throw ApiException.bad("campus", "campus 不合法");
    }
    return value;
  }
}
