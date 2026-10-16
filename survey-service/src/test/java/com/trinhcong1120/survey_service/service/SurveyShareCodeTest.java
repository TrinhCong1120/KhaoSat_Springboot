package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.survey.CreateSurveyRequest;
import com.trinhcong1120.survey_service.dto.survey.UpdateSurveyRequest;
import com.trinhcong1120.survey_service.dto.validation.QuestionValidationRuleResponse;
import com.trinhcong1120.survey_service.dto.validation.ValidationRuleCatalogResponse;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.entity.QuestionType;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.exception.SurveyApiException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SurveyShareCodeTest {
  private final SurveyRepository surveys = mock(SurveyRepository.class);
  private final SurveyScheduleService schedule = mock(SurveyScheduleService.class);
  private final SurveyAccessGuard guard = mock(SurveyAccessGuard.class);
  private final PageRepository pages = mock(PageRepository.class);
  private final MediaFileRepository media = mock(MediaFileRepository.class);
  private final QuestionRepository questions = mock(QuestionRepository.class);
  private final OptionRepository options = mock(OptionRepository.class);
  private final ConditionRepository conditions = mock(ConditionRepository.class);
  private final QuestionValidationRuleService rules = mock(QuestionValidationRuleService.class);
  private final SurveyAlertService alerts = mock(SurveyAlertService.class);
  private final SurveyService service = new SurveyService(
      surveys, guard, mock(SurveyAccessRepository.class), pages,
      questions, options,
      conditions, mock(ResponseRepository.class),
      mock(AnswerRepository.class), mock(AnswerOptionRepository.class),
      media, rules, schedule, alerts);

  @Test
  void deletingSomeoneElsesSurveyWithGlobalPermissionAlertsAfterDelete() {
    UUID surveyId = UUID.randomUUID(), ownerId = UUID.randomUUID(), actorId = UUID.randomUUID();
    Survey survey = new Survey(); survey.setId(surveyId); survey.setCreatorUserId(ownerId);
    when(surveys.findById(surveyId)).thenReturn(Optional.of(survey));
    when(guard.userId()).thenReturn(actorId);
    when(guard.has("survey_delete_all")).thenReturn(true);

    service.delete(surveyId);

    var order = inOrder(surveys, alerts);
    order.verify(surveys).delete(survey);
    order.verify(alerts).deletedByGlobalUser(survey, actorId);
  }

  @Test
  void ownerDeletingOwnSurveyDoesNotAlert() {
    UUID surveyId = UUID.randomUUID(), ownerId = UUID.randomUUID();
    Survey survey = new Survey(); survey.setId(surveyId); survey.setCreatorUserId(ownerId);
    when(surveys.findById(surveyId)).thenReturn(Optional.of(survey));
    when(guard.userId()).thenReturn(ownerId);

    service.delete(surveyId);

    verifyNoInteractions(alerts);
  }

  @Test
  void editSurveyContainsConfiguredRulesAndWholeCatalog() {
    UUID surveyId = UUID.randomUUID(), pageId = UUID.randomUUID(), questionId = UUID.randomUUID();
    Survey survey = new Survey(); survey.setId(surveyId);
    Page page = new Page(); page.setId(pageId); page.setSurvey(survey);
    QuestionType type = new QuestionType(); type.setId(UUID.randomUUID()); type.setCode("TEXT");
    Question question = new Question(); question.setId(questionId);
    question.setPage(page); question.setQuestionType(type);
    QuestionValidationRuleResponse configured = new QuestionValidationRuleResponse();
    configured.setCode("MIN_LENGTH");
    ValidationRuleCatalogResponse catalog = new ValidationRuleCatalogResponse();
    when(surveys.findById(surveyId)).thenReturn(Optional.of(survey));
    when(schedule.status(survey)).thenReturn(SurveyScheduleService.Status.DRAFT);
    when(pages.findBySurvey_IdOrderByOrderIndexAsc(surveyId)).thenReturn(List.of(page));
    when(questions.findByPage_IdOrderByOrderIndexAsc(pageId)).thenReturn(List.of(question));
    when(options.findByQuestion_IdOrderByOrderIndexAsc(questionId)).thenReturn(List.of());
    when(conditions.findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(surveyId, surveyId))
        .thenReturn(List.of());
    when(media.findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(any(), any())).thenReturn(List.of());
    when(rules.getByQuestion(questionId)).thenReturn(List.of(configured));
    when(rules.getCatalog(null)).thenReturn(catalog);

    var detail = service.getDetail(surveyId);
    assertSame(catalog, detail.getValidationRuleCatalog());
    assertEquals("MIN_LENGTH", detail.getPages().get(0).getQuestions().get(0)
        .getValidationRules().get(0).getCode());
    verify(rules).getCatalog(null);
    verify(rules).getByQuestion(questionId);
  }

  @Test
  void createGeneratesAnEightCharacterCode() {
    when(schedule.now()).thenReturn(Instant.now());
    when(schedule.status(any(Survey.class))).thenReturn(SurveyScheduleService.Status.DRAFT);
    when(surveys.save(any(Survey.class))).thenAnswer(call -> {
      Survey survey = call.getArgument(0);
      survey.setId(UUID.randomUUID());
      return survey;
    });
    assertTrue(service.create(new CreateSurveyRequest("Survey", null), "editor")
        .getShareCode().matches("[a-z0-9]{8}"));
  }

  @Test
  void updateRejectsInvalidAndTakenCodesWithoutSavingOtherFields() {
    UUID id = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setTitle("Old title");
    survey.setShareCode("abc1");
    when(surveys.findForWrite(id)).thenReturn(Optional.of(survey));
    when(surveys.existsByShareCode("Taken1")).thenReturn(true);

    for (String code : new String[] {null, "", "a b c", "cóDấu", "bad-code", "abc"}) {
      UpdateSurveyRequest request = new UpdateSurveyRequest("New title", null);
      request.setShareCode(code);
      SurveyApiException error = assertThrows(SurveyApiException.class,
          () -> service.update(id, request));
      assertEquals("INVALID_SHARE_CODE", error.getCode());
      assertEquals("Old title", survey.getTitle());
    }

    UpdateSurveyRequest duplicate = new UpdateSurveyRequest("New title", null);
    duplicate.setShareCode("Taken1");
    assertEquals("SHARE_CODE_TAKEN",
        assertThrows(SurveyApiException.class, () -> service.update(id, duplicate)).getCode());
    assertEquals("abc1", survey.getShareCode());
    verify(surveys, never()).save(any(Survey.class));
  }

  @Test
  void updateAllowsCaseDistinctCodeAndKeepsItWhenOmitted() {
    UUID id = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setId(id);
    survey.setTitle("Old title");
    survey.setShareCode("abc1");
    when(surveys.findForWrite(id)).thenReturn(Optional.of(survey));
    when(surveys.save(survey)).thenReturn(survey);
    when(schedule.status(survey)).thenReturn(SurveyScheduleService.Status.DRAFT);

    UpdateSurveyRequest changed = new UpdateSurveyRequest("New title", null);
    changed.setShareCode("Abc1");
    assertEquals("Abc1", service.update(id, changed).getShareCode());
    UpdateSurveyRequest unchanged = new UpdateSurveyRequest("Another title", null);
    assertEquals("Abc1", service.update(id, unchanged).getShareCode());
    verify(surveys).existsByShareCode("Abc1");
  }

  @Test
  void publicReferenceAcceptsCodeAndLegacyUuid() {
    UUID id = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setId(id);
    when(surveys.findByShareCode("Abc1")).thenReturn(Optional.of(survey));
    PublicSurveyService publicService = new PublicSurveyService(
        surveys, null, null, null, null, null, null, null, null, null, null,
        null, null, schedule);

    assertEquals(id, publicService.resolveSurveyId("Abc1"));
    assertEquals(id, publicService.resolveSurveyId(id.toString()));
    assertThrows(NotFoundException.class, () -> publicService.resolveSurveyId("abc1"));
  }

  @Test
  void updateJsonDistinguishesMissingCodeFromNull() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    UpdateSurveyRequest omitted = mapper.readValue("{\"title\":\"Survey\"}", UpdateSurveyRequest.class);
    UpdateSurveyRequest explicitNull = mapper.readValue(
        "{\"title\":\"Survey\",\"shareCode\":null}", UpdateSurveyRequest.class);
    assertFalse(omitted.isShareCodePresent());
    assertTrue(explicitNull.isShareCodePresent());
  }
}
