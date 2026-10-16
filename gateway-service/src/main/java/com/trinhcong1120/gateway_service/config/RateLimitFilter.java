package com.trinhcong1120.gateway_service.config;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RateLimitFilter implements WebFilter {

  private static final long SECOND = Duration.ofSeconds(1).toNanos();
  private static final long MINUTE = Duration.ofMinutes(1).toNanos();
  private static final long IDLE_EXPIRY = Duration.ofMinutes(2).toNanos();
  private static final Pattern IPV4 = Pattern.compile("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
  private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:.]{2,45}");
  private static final byte[] TOO_MANY_REQUESTS =
          "{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}"
                  .getBytes(StandardCharsets.UTF_8);

  private final Map<String, ClientCounters> clients = new LinkedHashMap<>(16, .75f, true);
  private final Map<Group, Limit> limits = new EnumMap<>(Group.class);
  private final LongSupplier ticker;
  private final MeterRegistry meterRegistry;
  private final boolean trustProxyHeader;
  private final int maxClients;

  @Autowired
  public RateLimitFilter(Environment environment, MeterRegistry meterRegistry) {
    this(environment, meterRegistry, System::nanoTime);
  }

  RateLimitFilter(Environment environment, MeterRegistry meterRegistry, LongSupplier ticker) {
    this.ticker = ticker;
    this.meterRegistry = meterRegistry;
    this.trustProxyHeader = environment.getProperty("app.rate-limit.trust-proxy-header", Boolean.class, false);
    this.maxClients = Math.max(1, environment.getProperty("app.rate-limit.max-clients", Integer.class, 20_000));
    for (Group group : Group.values()) {
      String key = "app.rate-limit." + group.name().toLowerCase().replace('_', '-');
      limits.put(group, new Limit(
              positive(environment, key + ".per-second", group.perSecond),
              positive(environment, key + ".per-minute", group.perMinute)));
    }
    meterRegistry.gauge("gateway.unique.clients.last.minute", this,
        filter -> filter.uniqueClientCount());
  }

  private static int positive(Environment environment, String key, int fallback) {
    return Math.max(1, environment.getProperty(key, Integer.class, fallback));
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    String method = exchange.getRequest().getMethod().name();
    Group group = group(method, path);
    String ip = clientIp(exchange);
    long retryNanos = check(ip, group, ticker.getAsLong());
    if (retryNanos == 0) {
      return chain.filter(exchange);
    }

    long retrySeconds = Math.max(1, (retryNanos + SECOND - 1) / SECOND);
    meterRegistry.counter("gateway.rate.limited", "group", group.name()).increment();
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    response.getHeaders().set("Retry-After", Long.toString(retrySeconds));
    return response.writeWith(Mono.just(response.bufferFactory().wrap(TOO_MANY_REQUESTS)));
  }

  private synchronized long check(String ip, Group group, long now) {
    // ponytail: one Gateway instance and a short critical section; use Redis if replicated.
    while (!clients.isEmpty()) {
      var first = clients.entrySet().iterator().next();
      if (now - first.getValue().lastSeen < IDLE_EXPIRY) break;
      clients.remove(first.getKey());
    }
    ClientCounters client = clients.get(ip);
    if (client == null) {
      if (clients.size() >= maxClients) {
        clients.remove(clients.keySet().iterator().next());
      }
      client = new ClientCounters();
      clients.put(ip, client);
    }
    client.lastSeen = now;
    long retry = client.global.record(now, limits.get(Group.GLOBAL));
    if (group != Group.GLOBAL) {
      retry = Math.max(retry, client.groups.computeIfAbsent(group, ignored -> new Counters())
              .record(now, limits.get(group)));
    }
    return retry;
  }

  private synchronized double uniqueClientCount() {
    long now = ticker.getAsLong();
    // ponytail: bounded single-Gateway estimate; aggregate centrally if Gateway is replicated.
    return clients.values().stream().filter(client -> now - client.lastSeen < MINUTE).count();
  }

  private String clientIp(ServerWebExchange exchange) {
    if (trustProxyHeader) {
      var values = exchange.getRequest().getHeaders().get("X-Real-IP");
      String header = values != null && values.size() == 1 ? values.get(0) : null;
      if (header != null && header.length() <= 45 && (IPV4.matcher(header).matches()
              || (header.indexOf(':') >= 0 && IPV6.matcher(header).matches()))) {
        try {
          return InetAddress.getByName(header).getHostAddress();
        } catch (Exception ignored) {
          // Invalid literal: fall back to the TCP peer.
        }
      }
    }
    var remote = exchange.getRequest().getRemoteAddress();
    return remote == null || remote.getAddress() == null
            ? "unknown" : remote.getAddress().getHostAddress();
  }

  private static Group group(String method, String path) {
    if ("OPTIONS".equals(method)) return Group.GLOBAL;
    if ("POST".equals(method) && "/api/Auth/login".equals(path)) return Group.LOGIN;
    if (path.startsWith("/api/public/surveys/")) {
      if ("POST".equals(method) && path.endsWith("/submit")) return Group.SUBMIT;
      if ("POST".equals(method) && path.endsWith("/validate")) return Group.VALIDATE;
      if ("POST".equals(method) && (path.endsWith("/sessions") || path.endsWith("/events")))
        return Group.TRACKING;
      if ("GET".equals(method)) return Group.SURVEY_GET;
    }
    if (path.startsWith("/api/survey-media/") || path.startsWith("/survey-media/")) {
      if ("GET".equals(method) || "HEAD".equals(method)) return Group.MEDIA_GET;
      if ("PUT".equals(method)) return Group.MEDIA_PUT;
    }
    if (path.startsWith("/swagger/") || path.startsWith("/swagger-ui")
            || path.startsWith("/api/Menus/")) return Group.OTHER_PUBLIC;
    return Group.GLOBAL;
  }

  private enum Group {
    GLOBAL(30, 300), LOGIN(2, 10), SUBMIT(2, 10), VALIDATE(3, 30), TRACKING(10, 120),
    SURVEY_GET(5, 60), MEDIA_GET(15, 120), MEDIA_PUT(2, 20), OTHER_PUBLIC(10, 120);

    final int perSecond;
    final int perMinute;

    Group(int perSecond, int perMinute) {
      this.perSecond = perSecond;
      this.perMinute = perMinute;
    }
  }

  private record Limit(int perSecond, int perMinute) {}

  private static final class ClientCounters {
    long lastSeen;
    final Counters global = new Counters();
    final Map<Group, Counters> groups = new EnumMap<>(Group.class);
  }

  private static final class Counters {
    long secondStart;
    long minuteStart;
    int secondCount;
    int minuteCount;

    long record(long now, Limit limit) {
      if (now - secondStart >= SECOND || secondCount == 0) {
        secondStart = now;
        secondCount = 0;
      }
      if (now - minuteStart >= MINUTE || minuteCount == 0) {
        minuteStart = now;
        minuteCount = 0;
      }
      if (secondCount < Integer.MAX_VALUE) secondCount++;
      if (minuteCount < Integer.MAX_VALUE) minuteCount++;
      long retry = 0;
      if (secondCount > limit.perSecond) retry = SECOND - (now - secondStart);
      if (minuteCount > limit.perMinute) retry = Math.max(retry, MINUTE - (now - minuteStart));
      return retry;
    }
  }
}
