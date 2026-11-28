package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import com.shu.campusbuddy.common.ErrorWriter;
import com.shu.campusbuddy.common.Hashes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtService jwt;
  private final UserRepository users;
  private final ErrorWriter errors;

  public JwtAuthFilter(JwtService jwt, UserRepository users, ErrorWriter errors) {
    this.jwt = jwt;
    this.users = users;
    this.errors = errors;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long started = System.nanoTime();
    String path = path(request);
    try {
      if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !path.startsWith("/api/")) {
        chain.doFilter(request, response);
        return;
      }
      String token = bearer(request);
      boolean open = isPublic(request.getMethod(), path);
      if (token == null) {
        if (!open) {
          errors.write(response, ApiException.unauthorized("未登录或令牌已过期"));
          return;
        }
        chain.doFilter(request, response);
        return;
      }
      try {
        authenticate(token);
      } catch (ApiException ex) {
        if (!open) {
          errors.write(response, ex);
          return;
        }
      }
      chain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - started) / 1_000_000L;
      String uid = AuthContext.find().map(user -> Hashes.shortHash(Long.toString(user.id()))).orElse("-");
      log.info("path={} status={} durationMs={} uidHash={}", path, response.getStatus(), durationMs, uid);
      AuthContext.clear();
    }
  }

  private void authenticate(String token) {
    JwtService.TokenClaims claims = jwt.parse(token);
    if (!"access".equals(claims.typ())) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
    UserAccount user = users.findById(claims.userId()).orElse(null);
    if (user == null || user.isDeleted()) {
      throw ApiException.unauthorized("未登录或令牌已过期");
    }
    if ("BANNED_ALL".equals(user.getAccountStatus())) {
      throw ApiException.forbidden("账号已封禁", "account");
    }
    boolean bound = user.getCampusId() != null && !user.getCampusId().isBlank();
    AuthContext.set(new CurrentUser(user.getId(), claims.role(), bound));
  }

  private static boolean isPublic(String method, String path) {
    if ("GET".equals(method) && "/api/v1/health".equals(path)) {
      return true;
    }
    if (!"POST".equals(method)) {
      return false;
    }
    return "/api/v1/auth/login".equals(path)
        || "/api/v1/auth/otp/send".equals(path)
        || "/api/v1/auth/refresh".equals(path);
  }

  private static String bearer(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return null;
    }
    String token = header.substring(7).trim();
    return token.isEmpty() ? null : token;
  }

  private static String path(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    if (context != null && !context.isEmpty() && uri.startsWith(context)) {
      return uri.substring(context.length());
    }
    return uri;
  }
}
