package com.trinhcong1120.survey_service.client;

import com.trinhcong1120.survey_service.dto.core.CoreUserResponse;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CoreUserClient {

  private final RestClient restClient;

  public CoreUserClient(RestClient.Builder restClientBuilder) {
    this.restClient =
            restClientBuilder
                    .baseUrl("http://core-service")
                    .build();
  }

  public CoreUserResponse getMe(String bearerToken) {
    if (bearerToken == null
            || bearerToken.isBlank()) {
      throw new BadRequestException(
              "Thieu Authorization header");
    }

    return restClient
            .get()
            .uri("/api/Users/me")
            .header("Authorization", bearerToken)
            .retrieve()
            .body(CoreUserResponse.class);
  }
}
