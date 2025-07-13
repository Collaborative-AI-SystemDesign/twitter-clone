package com.example.demo.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class JwtUtil {

    private final int ACCESS_TOKEN_EXPIRE_COUNT = 1000000;
    private final Key secretKey;
    private final String secret = "sKj2LmN9rpQ8WaBcXzOfHuGtYs5vDzP3";

    public JwtUtil() {
        this.secretKey = Keys.hmacShaKeyFor(this.secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, List<String> roles) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_COUNT);

        return Jwts.builder()
                .setSubject("AccessToken")
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validate(token);
    }

    private boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid Token: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAccessClaims(String token) {
        return extractClaims(token);
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJwt(token)
                .getBody();
    }

    private SecretKey getSecretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public void setTokenCookie(HttpServletResponse response, String token) {
        Cookie accessCookie = new Cookie("access_token", token);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setMaxAge(ACCESS_TOKEN_EXPIRE_COUNT/1000);
        response.addCookie(accessCookie);
    }
}
