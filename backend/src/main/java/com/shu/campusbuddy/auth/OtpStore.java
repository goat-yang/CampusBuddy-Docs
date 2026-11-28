package com.shu.campusbuddy.auth;

interface OtpStore {

  void issue(String campusId, String emailHash, String codeHash);

  void verifyAndConsume(String campusId, String emailHash, String codeHash);

  void clear(String campusId);
}
