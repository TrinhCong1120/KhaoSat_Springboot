package com.trinhcong1120.core_service.security;

import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header =
                request.getHeader(
                        "Authorization"
                );

        if (header == null
                || !header.startsWith(
                "Bearer "
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                header.substring(7);

        try {

            Claims claims =
                    jwtService.extractClaims(token);

            Object idClaim =
                    claims.get("id");

            if (idClaim == null) {

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                return;
            }

            Integer userId =
                    Integer.valueOf(
                            idClaim.toString()
                    );

            User user =
                    userRepository
                            .findByIdAndIsActiveTrue(
                                    userId
                            )
                            .orElse(null);

            if (user == null) {

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                return;
            }

            List<SimpleGrantedAuthority>
                    authorities =
                    new ArrayList<>();

            addAuthorities(
                    claims.get("permission"),
                    "",
                    authorities
            );

            addAuthorities(
                    claims.get("role"),
                    "ROLE_",
                    authorities
            );

            UsernamePasswordAuthenticationToken
                    authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            authorities
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(
                            authentication
                    );

            filterChain.doFilter(
                    request,
                    response
            );

        } catch (Exception ex) {

            SecurityContextHolder
                    .clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
        }
    }

    private void addAuthorities(
            Object claim,
            String prefix,
            List<SimpleGrantedAuthority>
                    authorities) {

        if (claim == null) {
            return;
        }

        if (claim instanceof Collection<?> values) {

            for (Object value : values) {

                authorities.add(
                        new SimpleGrantedAuthority(
                                prefix
                                        + value.toString()
                        )
                );
            }

            return;
        }

        authorities.add(
                new SimpleGrantedAuthority(
                        prefix
                                + claim.toString()
                )
        );
    }
}