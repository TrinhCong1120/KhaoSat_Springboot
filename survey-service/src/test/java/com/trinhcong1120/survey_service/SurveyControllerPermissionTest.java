package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trinhcong1120.survey_service.client.CoreUserClient;
import com.trinhcong1120.survey_service.controller.SurveyController;
import com.trinhcong1120.survey_service.service.SurveyService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(SurveyControllerPermissionTest.Config.class)
class SurveyControllerPermissionTest {
  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean SurveyService surveyService() { return mock(SurveyService.class); }
    @Bean SurveyController surveyController(SurveyService service) {
      return new SurveyController(service, mock(CoreUserClient.class));
    }
  }

  @Autowired private SurveyController controller;
  @Autowired private SurveyService service;

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void updateAndDeleteRequireTheirOwnPermissions() {
    UUID id = UUID.randomUUID();
    authenticate("survey_view");
    assertThrows(AccessDeniedException.class, () -> controller.update(id, null));
    assertThrows(AccessDeniedException.class, () -> controller.delete(id));
    verifyNoInteractions(service);

    authenticate("survey_update");
    controller.update(id, null);
    assertThrows(AccessDeniedException.class, () -> controller.delete(id));

    authenticate("survey_delete_all");
    controller.delete(id);
    verify(service).delete(id);
  }

  private void authenticate(String permission) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null,
            List.of(new SimpleGrantedAuthority(permission))));
  }
}
