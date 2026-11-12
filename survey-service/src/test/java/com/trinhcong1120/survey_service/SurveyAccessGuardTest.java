package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SurveyAccessGuardTest {
  private final UUID userId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final UUID surveyId = UUID.randomUUID();
  private final UUID childGroupId = UUID.randomUUID();
  private final SurveyRepository surveys = mock(SurveyRepository.class);
  private final GroupPermissions permissions = mock(GroupPermissions.class);
  private SurveyAccessGuard guard;

  @BeforeEach void setUp() {
    guard = new SurveyAccessGuard(surveys, mock(PageRepository.class),
        mock(QuestionRepository.class), mock(OptionRepository.class), mock(ConditionRepository.class),
        mock(ResponseRepository.class), mock(MediaFileRepository.class), permissions);
    Survey survey = new Survey();
    survey.setId(surveyId);
    survey.setCreatorUserId(ownerId);
    survey.setGroupId(childGroupId);
    when(surveys.findById(surveyId)).thenReturn(Optional.of(survey));
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of());
    when(permissions.survey(surveyId, childGroupId, ownerId)).thenReturn(Set.of());
    when(permissions.visibleSurveyIds(userId, false)).thenReturn(List.of());
  }

  @AfterEach void clear() { SecurityContextHolder.clearContext(); }

  @Test void childGroupRightWorksWithoutParentOrJwtRole() {
    authenticate(userId);
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of("EDIT"));
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertDoesNotThrow(() -> guard.edit(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));
  }

  @Test void directSurveyRightWorksWithoutGroupOrJwtRole() {
    authenticate(userId);
    when(permissions.group(childGroupId, userId)).thenReturn(Set.of());
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of("VIEW_RESPONSES", "MANAGE_ACCESS"));
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertDoesNotThrow(() -> guard.viewResponses(surveyId));
    assertDoesNotThrow(() -> guard.manageAccess(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.edit(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.exportData(surveyId));
  }

  @Test void creatorHasNoAutomaticRight() {
    authenticate(ownerId);
    assertThrows(AccessDeniedException.class, () -> guard.view(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.edit(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));
  }

  @Test void exportAndResponseViewRemainSeparateRights() {
    authenticate(userId);
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of("VIEW"));
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.viewResponses(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.exportData(surveyId));
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of("EXPORT_DATA"));
    assertThrows(AccessDeniedException.class, () -> guard.viewResponses(surveyId));
    assertDoesNotThrow(() -> guard.exportData(surveyId));
  }

  @Test void revokingGroupDoesNotRemoveDirectSurveyRight() {
    authenticate(userId);
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of("VIEW", "EDIT"));
    assertDoesNotThrow(() -> guard.edit(surveyId));
    when(permissions.survey(surveyId, childGroupId, userId)).thenReturn(Set.of("VIEW"));
    assertDoesNotThrow(() -> guard.view(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.edit(surveyId));
  }

  @Test void visibleListIsFilteredForScopedUser() {
    authenticate(userId);
    assertTrue(guard.visibleSurveys().isEmpty());
    verify(surveys, never()).findAllByOrderByCreatedAtDesc();
    when(permissions.visibleSurveyIds(userId, false)).thenReturn(List.of(surveyId));
    guard.visibleSurveys();
    verify(surveys).findByIdInOrderByCreatedAtDesc(List.of(surveyId));
  }

  @Test void globalUpdateDoesNotGrantDelete() {
    authenticate(userId, "survey_update_all");
    assertDoesNotThrow(() -> guard.edit(surveyId));
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));
  }

  @Test void staleManagerJwtCannotKeepRevokedGlobalDelete() {
    authenticate(userId, "survey_delete_all");
    assertThrows(AccessDeniedException.class, () -> guard.delete(surveyId));
    when(permissions.hasCurrentGlobalPermission(userId, "survey_delete_all")).thenReturn(true);
    assertDoesNotThrow(() -> guard.delete(surveyId));
  }

  private void authenticate(UUID id, String... roles) {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        id.toString(), null, Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
  }
}
