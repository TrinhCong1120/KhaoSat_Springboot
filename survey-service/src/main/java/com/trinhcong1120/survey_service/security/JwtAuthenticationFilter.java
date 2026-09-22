package com.trinhcong1120.survey_service.security;

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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain
  ) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    try {

      Claims claims = jwtService.extractClaims(token);

      String userId = claims.get("id", String.class);

      if (userId == null) {
        Object idObject = claims.get("id");

        if (idObject != null) {
          userId = String.valueOf(idObject);
        }
      }

      if (userId == null) {
        filterChain.doFilter(request, response);
        return;
      }

      List<SimpleGrantedAuthority> authorities =
              new ArrayList<>();

      addPermissions(
              authorities,
              claims.get("permission")
      );

      addPermissions(
              authorities,
              claims.get("permissions")
      );

      addRoles(
              authorities,
              claims.get("role")
      );

      addRoles(
              authorities,
              claims.get("roles")
      );

      UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                      userId,
                      null,
                      authorities
              );

      SecurityContextHolder.getContext()
              .setAuthentication(authentication);

    } catch (Exception e) {

      SecurityContextHolder.clearContext();

    }

    filterChain.doFilter(request, response);
  }

  private void addPermissions(
          List<SimpleGrantedAuthority> authorities,
          Object value
  ) {

    if (value == null) {
      return;
    }

    if (value instanceof Collection<?> collection) {

      for (Object item : collection) {

        if (item != null) {
          authorities.add(
                  new SimpleGrantedAuthority(
                          String.valueOf(item)
                  )
          );
        }
      }

      return;
    }

    authorities.add(
            new SimpleGrantedAuthority(
                    String.valueOf(value)
            )
    );
  }

  private void addRoles(
          List<SimpleGrantedAuthority> authorities,
          Object value
  ) {

    if (value == null) {
      return;
    }

    if (value instanceof Collection<?> collection) {

      for (Object item : collection) {

        if (item != null) {
          addRole(
                  authorities,
                  String.valueOf(item)
          );
        }
      }

      return;
    }

    addRole(
            authorities,
            String.valueOf(value)
    );
  }

  private void addRole(
          List<SimpleGrantedAuthority> authorities,
          String role
  ) {

    if (role.startsWith("ROLE_")) {
      authorities.add(
              new SimpleGrantedAuthority(role)
      );
    } else {
      authorities.add(
              new SimpleGrantedAuthority(
                      "ROLE_" + role
              )
      );
    }
  }
}