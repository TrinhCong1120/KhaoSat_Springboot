package com.trinhcong1120.gateway_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RateLimitFilterTest {

  private final AtomicLong time = new AtomicLong();

  @Test
  void limitsPerSecondAndMinuteThenResets() {
    var environment = new MockEnvironment()
            .withProperty("app.rate-limit.survey-get.per-second", "2")
            .withProperty("app.rate-limit.survey-get.per-minute", "4");
    var filter = new RateLimitFilter(environment, new SimpleMeterRegistry(), time::get);

    assertEquals(200, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "1.2.3.4", null));
    assertEquals(429, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "1.2.3.4", null));
    time.addAndGet(1_000_000_000L);
    assertEquals(200, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "1.2.3.4", null));
    assertEquals(429, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "5.6.7.8", null));
    time.addAndGet(59_000_000_000L);
    assertEquals(200, request(filter, HttpMethod.GET, "/api/public/surveys/abc", "1.2.3.4", null));
  }

  @Test
  void separatesLoginSubmitMediaAndPreflight() {
    var filter = new RateLimitFilter(new MockEnvironment(), new SimpleMeterRegistry(), time::get);
    for (int i = 0; i < 2; i++) {
      assertEquals(200, request(filter, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", null));
      assertEquals(200, request(filter, HttpMethod.POST, "/api/public/surveys/x/submit", "1.2.3.4", null));
      assertEquals(200, request(filter, HttpMethod.PUT, "/api/survey-media/x", "1.2.3.4", null));
    }
    assertEquals(429, request(filter, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", null));
    assertEquals(429, request(filter, HttpMethod.POST, "/api/public/surveys/x/submit", "1.2.3.4", null));
    assertEquals(429, request(filter, HttpMethod.PUT, "/api/survey-media/x", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.GET, "/survey-media/x", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.OPTIONS, "/api/public/surveys/x", "1.2.3.4", null));
  }

  @Test
  void sessionsAndEventsShareTrackingLimitWithoutBlockingSubmit() {
    var filter = new RateLimitFilter(new MockEnvironment()
        .withProperty("app.rate-limit.tracking.per-second", "1"),
        new SimpleMeterRegistry(), time::get);
    assertEquals(200, request(filter, HttpMethod.POST, "/api/public/surveys/x/sessions", "1.2.3.4", null));
    assertEquals(429, request(filter, HttpMethod.POST, "/api/public/surveys/x/events", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.POST, "/api/public/surveys/x/submit", "1.2.3.4", null));
  }

  @Test
  void onlyTrustsValidProxyHeaderWhenConfigured() {
    var direct = new RateLimitFilter(new MockEnvironment()
            .withProperty("app.rate-limit.login.per-second", "1"), new SimpleMeterRegistry(), time::get);
    assertEquals(200, request(direct, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "5.6.7.8"));
    assertEquals(429, request(direct, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "9.8.7.6"));

    var proxied = new RateLimitFilter(new MockEnvironment()
            .withProperty("app.rate-limit.trust-proxy-header", "true")
            .withProperty("app.rate-limit.login.per-second", "1"), new SimpleMeterRegistry(), time::get);
    assertEquals(200, request(proxied, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "5.6.7.8"));
    assertEquals(200, request(proxied, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "9.8.7.6"));
    assertEquals(429, request(proxied, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "5.6.7.8"));
    assertEquals(200, request(proxied, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "bad.example"));
    assertEquals(429, request(proxied, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", "8.8.8.8, 9.9.9.9"));
  }

  @Test
  void globalLimitAndBoundedClientCache() {
    var filter = new RateLimitFilter(new MockEnvironment()
            .withProperty("app.rate-limit.global.per-second", "2")
            .withProperty("app.rate-limit.max-clients", "1"), new SimpleMeterRegistry(), time::get);
    assertEquals(200, request(filter, HttpMethod.GET, "/api/Pages/x", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.GET, "/api/Surveys/x", "1.2.3.4", null));
    assertEquals(429, request(filter, HttpMethod.GET, "/api/Questions/x", "1.2.3.4", null));
    assertEquals(200, request(filter, HttpMethod.GET, "/api/Surveys/x", "5.6.7.8", null));
    assertEquals(200, request(filter, HttpMethod.GET, "/api/Surveys/x", "1.2.3.4", null));
  }

  @Test
  void concurrentRequestsCannotExceedLimit() {
    var filter = new RateLimitFilter(new MockEnvironment(), new SimpleMeterRegistry(), time::get);
    var accepted = new AtomicInteger();
    IntStream.range(0, 30).parallel().forEach(i -> {
      if (request(filter, HttpMethod.POST, "/api/Auth/login", "1.2.3.4", null) == 200) {
        accepted.incrementAndGet();
      }
    });
    assertEquals(2, accepted.get());
  }

  @Test
  void rateLimitResponseKeepsCorsHeaders() {
    var environment = new MockEnvironment()
            .withProperty("app.rate-limit.login.per-second", "1");
    var cors = new CorsConfig(environment).corsWebFilter();
    var filter = new RateLimitFilter(environment, new SimpleMeterRegistry(), time::get);
    for (int i = 0; i < 2; i++) {
      var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("http://localhost:8080/api/Auth/login")
              .remoteAddress(new InetSocketAddress("1.2.3.4", 1234))
              .header(HttpHeaders.ORIGIN, "http://localhost:3000"));
      cors.filter(exchange, e -> filter.filter(e, ignored -> reactor.core.publisher.Mono.empty())).block();
      if (i == 1) {
        assertEquals(429, exchange.getResponse().getStatusCode().value());
        assertEquals("http://localhost:3000",
                exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
      }
    }
  }

  private static int request(RateLimitFilter filter, HttpMethod method, String path,
                             String ip, String realIp) {
    var builder = MockServerHttpRequest.method(method, path)
            .remoteAddress(new InetSocketAddress(ip, 1234));
    if (realIp != null) builder.header("X-Real-IP", realIp);
    var exchange = MockServerWebExchange.from(builder);
    filter.filter(exchange, ignored -> reactor.core.publisher.Mono.empty()).block();
    if (exchange.getResponse().getStatusCode() == null) return 200;
    int status = exchange.getResponse().getStatusCode().value();
    if (status == 429) {
      assertTrue(exchange.getResponse().getHeaders().getFirst("Retry-After") != null);
      assertTrue(exchange.getResponse().getBodyAsString().block().contains("RATE_LIMITED"));
    }
    return status;
  }
}
