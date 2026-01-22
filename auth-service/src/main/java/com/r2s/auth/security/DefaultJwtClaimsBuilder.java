package com.r2s.auth.security;

import com.r2s.core.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultJwtClaimsBuilder implements JwtClaimsBuilder {

    @Override
    public Map<String, Object> buildClaims(User user) {
        String enumName = user.getRole().name();
        String roleClaim = enumName.startsWith("ROLE_") ? enumName : "ROLE_" + enumName;
        return Map.of("role", roleClaim);
    }
}
