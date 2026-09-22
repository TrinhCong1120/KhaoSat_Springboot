package com.trinhcong1120.auth_service.security;

import com.trinhcong1120.auth_service.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.remember-expiration}")
    private long rememberExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(
            User user,
            List<String> roles,
            List<String> permissions,
            boolean rememberMe) {

        Date now = new Date();

        long expirationTime = rememberMe
                ? rememberExpiration
                : expiration;

        Date expirationDate =
                new Date(now.getTime() + expirationTime);

        return Jwts.builder()

                .subject(user.getUsername())

                .claim("id", user.getId().toString())
                .claim("name", user.getUsername())
                .claim("role", roles)
                .claim("permission", permissions)

                .issuer(issuer)

                .audience()
                .add(audience)
                .and()

                .issuedAt(now)
                .expiration(expirationDate)

                .signWith(getSigningKey())

                .compact();
    }
}