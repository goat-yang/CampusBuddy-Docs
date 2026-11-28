package com.shu.campusbuddy.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailDomainPolicyTest {

  @Test
  void acceptsShuDomainsOnly() {
    assertTrue(EmailDomainPolicy.allowed("24123456@shu.edu.cn"));
    assertTrue(EmailDomainPolicy.allowed("A@SHU.EDU.CN"));
    assertTrue(EmailDomainPolicy.allowed("t@staff.shu.edu.cn"));
    assertTrue(EmailDomainPolicy.allowed("t@mail.shu.edu.cn"));
    assertTrue(EmailDomainPolicy.allowed("t@oa.shu.edu.cn"));
    assertTrue(EmailDomainPolicy.allowed("t@department.shu.edu.cn"));
    assertFalse(EmailDomainPolicy.allowed("a@gmail.com"));
    assertFalse(EmailDomainPolicy.allowed("a@cs.shu.edu.cn"));
    assertFalse(EmailDomainPolicy.allowed("a@shu.edu.cn.evil.com"));
    assertFalse(EmailDomainPolicy.allowed("shu.edu.cn"));
  }

  @Test
  void masksCampusIdAndEmail() {
    assertEquals("2412****", Masks.campusId("24123456"));
    assertEquals("", Masks.campusId(null));
    assertEquals("2412****@shu.edu.cn", Masks.email("24123456@shu.edu.cn"));
    assertEquals("", Masks.email(""));
  }

  @Test
  void mapsCreditLevels() {
    assertEquals("EXCELLENT", CreditLevels.of(100));
    assertEquals("EXCELLENT", CreditLevels.of(90));
    assertEquals("GOOD", CreditLevels.of(75));
    assertEquals("NORMAL", CreditLevels.of(60));
    assertEquals("RESTRICTED", CreditLevels.of(59));
  }
}
