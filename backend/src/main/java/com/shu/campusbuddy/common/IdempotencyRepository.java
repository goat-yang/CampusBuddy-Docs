package com.shu.campusbuddy.common;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

  Optional<IdempotencyRecord> findByUserIdAndPathAndIdemKey(Long userId, String path, String idemKey);
}
