package com.trinhcong1120.gateway_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

  public CorsConfig(Environment environment) {
    Binder binder = Binder.get(environment);

    this.allowedOrigins = binder
            .bind("app.cors.allowed-origins", Bindable.listOf(String.class))
            .orElse(List.of("http://localhost:3000"));
    this.allowedMethods = binder
            .bind("app.cors.allowed-methods", Bindable.listOf(String.class))
            .orElse(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    this.allowedHeaders = binder
            .bind("app.cors.allowed-headers", Bindable.listOf(String.class))
            .orElse(List.of("*"));
    this.exposedHeaders = binder
            .bind("app.cors.exposed-headers", Bindable.listOf(String.class))
            .orElse(List.of("Authorization"));
    this.allowCredentials = binder
            .bind("app.cors.allow-credentials", Boolean.class)
            .orElse(true);
    this.maxAge = binder
            .bind("app.cors.max-age", Long.class)
            .orElse(3600L);
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
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
