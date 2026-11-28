package com.shu.campusbuddy.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<InterestTag, Long> {

  List<InterestTag> findByIdInAndDeletedFalse(Collection<Long> ids);
}
