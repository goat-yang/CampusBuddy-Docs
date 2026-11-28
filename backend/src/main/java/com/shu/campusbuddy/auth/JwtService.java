package com.shu.campusbuddy.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.shu.campusbuddy.common.ApiException;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  static final String ISSUER = "campusbuddy";

  private final CampusBuddyProperties properties;
  private Algorithm algorithm;

  public JwtService(CampusBuddyProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void checkSecret() {
    String secret = properties.getJwt().getSecret();
    if (secret == null || secret.length() < 32 || "secret".equals(secret)) {
      throw new IllegalStateException("JWT_SECRET 必须是 32 位以上随机串，不能使用 secret");
    }
    if (properties.getJwt().getAccessTtlSec() <= 0 || properties.getJwt().getAccessTtlSec() > 7200) {
      throw new IllegalStateException("access token 有效期必须在 1 到 7200 秒之间");
    }
    algorithm = Algorithm.HMAC256(secret);
  }

  public String sign(long userId, String role, String typ, String jti, Instant expiresAt) {
    var builder =
        JWT.create()
            .withIssuer(ISSUER)
            .withSubject(Long.toString(userId))
            .withClaim("role", role)
            .withClaim("typ", typ)
            .withExpiresAt(expiresAt)
            .withIssuedAt(Instant.now());
    if (jti != null) {
      builder.withJWTId(jti);
    }
    return builder.sign(algorithm);
  }

  public TokenClaims parse(String token) {
    try {
      DecodedJWT jwt = JWT.require(algorithm).withIssuer(ISSUER).build().verify(token);
      String role = jwt.getClaim("role").asString();
      String typ = jwt.getClaim("typ").asString();
      if (role == null || typ == null) {
        throw ApiException.unauthorized("未登录或令牌已过期");
      }
      return new TokenClaims(Long.parseLong(jwt.getSubject()), role, typ, jwt.getId());
    } catch (JWTVerificationException | NumberFormatException ex) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
  }

  public String pepper() {
    return properties.getJwt().getSecret();
  }

  public record TokenClaims(long userId, String role, String typ, String jti) {}
}
