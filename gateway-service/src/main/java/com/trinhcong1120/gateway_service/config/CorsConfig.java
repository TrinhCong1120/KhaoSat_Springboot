package com.trinhcong1120.gateway_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

  private final List<String> allowedOrigins;
  private final List<String> allowedMethods;
  private final List<String> allowedHeaders;
  private final List<String> exposedHeaders;
  private final Boolean allowCredentials;
  private final Long maxAge;

  public CorsConfig(
          @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins,
          @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}") List<String> allowedMethods,
          @Value("${app.cors.allowed-headers:*}") List<String> allowedHeaders,
          @Value("${app.cors.exposed-headers:Authorization}") List<String> exposedHeaders,
          @Value("${app.cors.allow-credentials:true}") Boolean allowCredentials,
          @Value("${app.cors.max-age:3600}") Long maxAge
  ) {
    this.allowedOrigins = allowedOrigins;
    this.allowedMethods = allowedMethods;
    this.allowedHeaders = allowedHeaders;
    this.exposedHeaders = exposedHeaders;
    this.allowCredentials = allowCredentials;
    this.maxAge = maxAge;
  }

  @Bean
  public CorsWebFilter corsWebFilter() {

    CorsConfiguration configuration =
            new CorsConfiguration();

    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(allowedMethods);
    configuration.setAllowedHeaders(allowedHeaders);
    configuration.setExposedHeaders(exposedHeaders);
    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(maxAge);

    UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
            "/**",
            configuration
    );

    return new CorsWebFilter(source);
  }
}
