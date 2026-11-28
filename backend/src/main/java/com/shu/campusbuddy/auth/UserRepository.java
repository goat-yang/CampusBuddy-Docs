package com.shu.campusbuddy.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByCampusIdAndDeletedFalse(String campusId);

  Optional<UserAccount> findByOpenidAndDeletedFalse(String openid);

  Optional<UserAccount> findBySchoolEmailAndDeletedFalse(String schoolEmail);
}
