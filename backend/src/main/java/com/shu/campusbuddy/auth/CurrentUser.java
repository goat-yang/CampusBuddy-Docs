package com.shu.campusbuddy.auth;

public record CurrentUser(long id, String role, boolean campusBound) {}
