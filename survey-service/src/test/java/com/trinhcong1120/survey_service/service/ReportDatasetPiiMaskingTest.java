package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.dto.report.SurveyDatasetResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.access.AccessDeniedException;

class ReportDatasetPiiMaskingTest {

  private SurveyRepository surveyRepo;
  private SurveyAccessGuard guard;
  private PageRepository pageRepo;
  private QuestionRepository questionRepo;
  private ResponseRepository responseRepo;
  private AnswerRepository answerRepo;
  private AnswerOptionRepository answerOptionRepo;
  private StatisticsService statisticsService;
  private QuestionValidationRuleRepository validationRuleRepo;
  private PiiDetectionService piiService;
  private JdbcTemplate jdbcTemplate;
  private ReportService reportService;

  private UUID surveyId;
  private UUID phoneQId;
  private UUID emailQId;
  private UUID scoreQId;
  private UUID childScoreQId;
  private UUID classQId;
  private UUID responseId;

  @BeforeEach
  void setUp() {
    surveyRepo = mock(SurveyRepository.class);
    guard = mock(SurveyAccessGuard.class);
    pageRepo = mock(PageRepository.class);
    questionRepo = mock(QuestionRepository.class);
    responseRepo = mock(ResponseRepository.class);
    answerRepo = mock(AnswerRepository.class);
    answerOptionRepo = mock(AnswerOptionRepository.class);
    statisticsService = mock(StatisticsService.class);
    validationRuleRepo = mock(QuestionValidationRuleRepository.class);
    piiService = new PiiDetectionService(validationRuleRepo);
    jdbcTemplate = mock(JdbcTemplate.class);
    doNothing().when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any());

    reportService = new ReportService(
        surveyRepo, guard, pageRepo, questionRepo, responseRepo,
        answerRepo, answerOptionRepo, statisticsService, piiService, jdbcTemplate
    );

    surveyId = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setId(surveyId);
    survey.setTitle("Khao sat chung");
    when(surveyRepo.findById(surveyId)).thenReturn(Optional.of(survey));

    QuestionType textType = new QuestionType();
    textType.setCode("SHORT_TEXT");
    QuestionType numType = new QuestionType();
    numType.setCode("NUMBER");

    phoneQId = UUID.randomUUID();
    Question phoneQ = new Question();
    phoneQ.setId(phoneQId);
    phoneQ.setQuestionText("Liên hệ nhanh");
    phoneQ.setQuestionType(textType);

    emailQId = UUID.randomUUID();
    Question emailQ = new Question();
    emailQ.setId(emailQId);
    emailQ.setQuestionText("Email nhận thông báo");
    emailQ.setQuestionType(textType);

    classQId = UUID.randomUUID();
    Question classQ = new Question();
    classQ.setId(classQId);
    classQ.setQuestionText("Phòng ban / Đơn vị");
    classQ.setQuestionType(textType);

    scoreQId = UUID.randomUUID();
    Question scoreQ = new Question();
    scoreQ.setId(scoreQId);
    scoreQ.setQuestionText("Điểm tổng");
    scoreQ.setQuestionType(numType);

    childScoreQId = UUID.randomUUID();
    Question childQ = new Question();
    childQ.setId(childScoreQId);
    childQ.setQuestionText("Điểm thành phần");
    childQ.setQuestionType(numType);
    childQ.setParent(scoreQ);

    when(questionRepo.findByPage_Survey_Id(surveyId))
        .thenReturn(List.of(phoneQ, emailQ, classQ, scoreQ, childQ));

    QuestionValidationRule phoneRule = new QuestionValidationRule();
    phoneRule.setId(new QuestionValidationRuleId(phoneQId, "PHONE"));
    phoneRule.setQuestion(phoneQ);
    when(validationRuleRepo.findByQuestion_Page_Survey_IdAndIsActiveTrue(surveyId))
        .thenReturn(List.of(phoneRule));

    responseId = UUID.randomUUID();
    Response resp = new Response();
    resp.setId(responseId);
    resp.setRequestId(UUID.randomUUID());
    resp.setSubmittedAt(LocalDateTime.now());
    when(responseRepo.findBySurvey_IdOrderBySubmittedAtDesc(surveyId)).thenReturn(List.of(resp));

    Answer aPhone = new Answer();
    aPhone.setId(UUID.randomUUID());
    aPhone.setQuestion(phoneQ);
    aPhone.setAnswerText("0912345678");

    Answer aEmail = new Answer();
    aEmail.setId(UUID.randomUUID());
    aEmail.setQuestion(emailQ);
    aEmail.setAnswerText("an.nguyen@company.vn");

    Answer aDept = new Answer();
    aDept.setId(UUID.randomUUID());
    aDept.setQuestion(classQ);
    aDept.setAnswerText("Kinh doanh");

    Answer aRootNum = new Answer();
    aRootNum.setId(UUID.randomUUID());
    aRootNum.setQuestion(scoreQ);
    aRootNum.setAnswerNumber(BigDecimal.valueOf(8.5));

    Answer aChildNum = new Answer();
    aChildNum.setId(UUID.randomUUID());
    aChildNum.setQuestion(childQ);
    aChildNum.setAnswerNumber(BigDecimal.valueOf(4.0));

    when(answerRepo.findByResponse_Id(responseId))
        .thenReturn(List.of(aPhone, aEmail, aDept, aRootNum, aChildNum));
  }

  @Test
  void masksPiiByDefaultAndAllowsUnmaskWithExportPermission() {
    when(guard.can(surveyId, "EXPORT_DATA")).thenReturn(true);

    SurveyDatasetResponse masked = reportService.getSurveyDataset(surveyId, false);
    assertFalse(masked.isPiiIncluded());
    assertTrue(masked.isCanExportData());
    assertTrue(masked.getPiiQuestionIds().contains(phoneQId));
    assertTrue(masked.getPiiQuestionIds().contains(emailQId));
    assertFalse(masked.getPiiQuestionIds().contains(classQId));

    SurveyDatasetResponse.RecordItem rec = masked.getRecords().get(0);
    assertEquals("091***78", rec.getAnswers().get(phoneQId.toString()).getText());
    assertEquals("an***@company.vn", rec.getAnswers().get(emailQId.toString()).getText());
    assertEquals("Kinh doanh", rec.getAnswers().get(classQId.toString()).getText());
    // totalScore should only count root numeric question (8.5), not double-count childQ (4.0)
    assertEquals(8.5, rec.getTotalScore(), 0.001);

    SurveyDatasetResponse unmasked = reportService.getSurveyDataset(surveyId, true);
    verify(guard).exportData(surveyId);
    assertTrue(unmasked.isPiiIncluded());
    assertEquals("0912345678", unmasked.getRecords().get(0).getAnswers().get(phoneQId.toString()).getText());
    assertEquals("an.nguyen@company.vn", unmasked.getRecords().get(0).getAnswers().get(emailQId.toString()).getText());
  }

  @Test
  void includePiiThrowsWhenExportPermissionDenied() {
    doThrow(new AccessDeniedException("No export")).when(guard).exportData(surveyId);
    assertThrows(AccessDeniedException.class, () -> reportService.getSurveyDataset(surveyId, true));
  }
}

