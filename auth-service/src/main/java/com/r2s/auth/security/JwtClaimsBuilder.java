package com.r2s.auth.security;

import com.r2s.core.entity.User;

import java.util.Map;

public interface JwtClaimsBuilder {
    Map<String, Object> buildClaims(User user);
}
