package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinhcong1120.survey_service.controller.SurveyAccessController;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import com.trinhcong1120.survey_service.service.SurveyAccessService;
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

@SpringJUnitConfig(SurveyAccessControllerPermissionTest.Config.class)
class SurveyAccessControllerPermissionTest {
  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean SurveyAccessGuard surveyAccessGuard() { return mock(SurveyAccessGuard.class); }
    @Bean SurveyAccessService accessService() { return mock(SurveyAccessService.class); }
    @Bean SurveyAccessController accessController(SurveyAccessService service) {
      return new SurveyAccessController(service);
    }
  }

  @Autowired private SurveyAccessController controller;
  @Autowired private SurveyAccessGuard guard;
  @Autowired private SurveyAccessService service;

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void controllerUsesSurveyAccessPolicy() {
    UUID surveyId = UUID.randomUUID();
    authenticate();
    assertThrows(AccessDeniedException.class, () -> controller.list(surveyId));

    when(guard.canManageAccess(surveyId)).thenReturn(true);
    authenticate("survey_update");
    controller.list(surveyId);
    verify(service).list(surveyId);

    authenticate("survey_update_all");
    controller.changeOwner(surveyId, null);
    verify(service).changeOwner(surveyId, null);
  }

  private void authenticate(String... permissions) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null,
            java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
  }
}
