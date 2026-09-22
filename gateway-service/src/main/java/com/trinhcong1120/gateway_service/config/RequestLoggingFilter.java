package com.trinhcong1120.gateway_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements WebFilter {

  private static final Logger log =
          LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    StopWatch stopWatch = new StopWatch();

    String method = exchange.getRequest().getMethod().name();
    String path = exchange.getRequest().getURI().getRawPath();
    String query = exchange.getRequest().getURI().getRawQuery();
    String pathWithQuery = query == null ? path : path + "?" + query;
    String remoteAddress = exchange.getRequest().getRemoteAddress() == null
            ? "unknown"
            : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

    log.info(
            "Gateway request started: {} {} from {}",
            method,
            pathWithQuery,
            remoteAddress
    );

    stopWatch.start();

    return chain.filter(exchange)
            .doFinally(signalType -> {
      if (stopWatch.isRunning()) {
        stopWatch.stop();
      }

              int status = exchange.getResponse().getStatusCode() == null
                      ? 200
                      : exchange.getResponse().getStatusCode().value();

      log.info(
              "Gateway request completed: {} {} -> {} in {} ms",
              method,
                      pathWithQuery,
                      status,
              stopWatch.getTotalTimeMillis()
      );
            });
  }
}
