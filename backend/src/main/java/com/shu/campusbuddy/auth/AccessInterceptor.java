package com.shu.campusbuddy.auth;

import com.shu.campusbuddy.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod method)) {
      return true;
    }
    RequireRole role = method.getMethodAnnotation(RequireRole.class);
    if (role == null) {
      role = method.getBeanType().getAnnotation(RequireRole.class);
    }
    RequireCampusBound campus = method.getMethodAnnotation(RequireCampusBound.class);
    if (campus == null) {
      campus = method.getBeanType().getAnnotation(RequireCampusBound.class);
    }
    if (role == null && campus == null) {
      return true;
    }
    CurrentUser user = AuthContext.require();
    if (role != null && !role.value().equals(user.role())) {
      throw ApiException.forbidden("无权访问", "role");
    }
    if (campus != null && !user.campusBound()) {
      throw ApiException.forbidden("未绑定学号，请先绑定学号后再报名或发布", "campusId");
    }
    return true;
  }
}
