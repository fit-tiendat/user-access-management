package com.r2s.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtService {

    private final Key key;

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret); // decode Base64
        this.key = Keys.hmacShaKeyFor(keyBytes);         // tạo HMAC key đúng chuẩn
    }

    // ✅ username = subject
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    // giữ parse để tái dùng
    private Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    // NEW: đọc role từ claim đã được auth-service nhúng vào JWT
    public String extractRole(String token) {
        Object role = parse(token).get("role");
        return role == null ? null : role.toString(); // ví dụ: "ROLE_ADMIN"
    }
}
