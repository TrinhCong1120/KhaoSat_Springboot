package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.entity.SurveyAccess;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SurveyAccessGuardTest {
  private final UUID ownerId = UUID.randomUUID();
  private final UUID editorId = UUID.randomUUID();
  private final UUID surveyId = UUID.randomUUID();
  private final SurveyRepository surveys = mock(SurveyRepository.class);
  private final SurveyAccessRepository accesses = mock(SurveyAccessRepository.class);
  private SurveyAccessGuard guard;

  @BeforeEach
  void setUp() {
    guard = new SurveyAccessGuard(surveys, accesses, mock(PageRepository.class),
        mock(QuestionRepository.class), mock(OptionRepository.class),
        mock(ConditionRepository.class), mock(ResponseRepository.class),
        mock(MediaFileRepository.class));
    Survey survey = new Survey();
    survey.setId(surveyId);
    survey.setCreatorUserId(ownerId);
    when(surveys.findById(surveyId)).thenReturn(Optional.of(survey));
    SurveyAccess access = new SurveyAccess();
    access.setCanView(true);
    access.setCanEdit(true);
    when(accesses.findBySurvey_IdAndUserId(surveyId, editorId)).thenReturn(Optional.of(access));
  }

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void editorAccessAllowsViewAndEditButNotSurveyDeletion() {
    authenticate(editorId);
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertDoesNotThrow(() -> guard.edit(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.manageAccess(surveyId));

  }

  @Test
  void ownerPassesObjectChecks() {
    authenticate(ownerId);
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertDoesNotThrow(() -> guard.edit(surveyId));
    assertDoesNotThrow(() -> guard.delete(surveyId));
    assertDoesNotThrow(() -> guard.manageAccess(surveyId));
  }

  @Test
  void globalDeleteNeedsItsOwnPermission() {
    authenticate(editorId, "survey_update_all");
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertDoesNotThrow(() -> guard.edit(surveyId));
    assertDoesNotThrow(() -> guard.manageAccess(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));

    authenticate(editorId, "survey_update_all", "survey_delete");
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));

    authenticate(editorId, "survey_delete_all");
    assertDoesNotThrow(() -> guard.delete(surveyId));
  }

  @Test
  void globalViewDoesNotGrantEditWithoutAccess() {
    authenticate(UUID.randomUUID(), "survey_view_all");
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.edit(surveyId));
  }

  @Test
  void unrelatedUserCannotViewOrEdit() {
    authenticate(UUID.randomUUID());
    assertThrows(AccessDeniedException.class, () -> guard.view(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.edit(surveyId));
  }

  private void authenticate(UUID userId, String... permissions) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userId.toString(), null,
            Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
  }
}
