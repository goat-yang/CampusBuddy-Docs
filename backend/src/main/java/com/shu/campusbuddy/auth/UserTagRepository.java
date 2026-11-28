package com.shu.campusbuddy.auth;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserTagRepository extends JpaRepository<UserTagLink, UserTagKey> {

  List<UserTagLink> findByUserIdOrderBySortNoAsc(Long userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from UserTagLink link where link.userId = :userId")
  void deleteByUserId(Long userId);
}
