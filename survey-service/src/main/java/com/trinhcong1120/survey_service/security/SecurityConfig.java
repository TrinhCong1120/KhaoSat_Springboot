package com.trinhcong1120.survey_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(
          JwtAuthenticationFilter jwtAuthenticationFilter
  ) {
    this.jwtAuthenticationFilter =
            jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
          HttpSecurity http
  ) throws Exception {

    http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .authorizeHttpRequests(auth -> auth

                    /*
                     * Public Survey
                     */
                    .requestMatchers(
                            "/api/public/**"
                    ).permitAll()

                    /*
                     * Condition API của hệ thống cũ
                     * hiện tại đang public.
                     */
                    .requestMatchers(
                            "/api/Conditions/**",
                            "/api/conditions/**"
                    ).permitAll()

                    /*
                     * Swagger
                     */
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**"
                    ).permitAll()
                    /*
                     * Các API còn lại yêu cầu JWT
                     */
                    .anyRequest()
                    .authenticated()
            )

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
