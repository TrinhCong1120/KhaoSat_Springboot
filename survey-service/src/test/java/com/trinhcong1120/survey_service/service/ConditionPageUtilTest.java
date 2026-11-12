package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionPageUtilTest {

  @Test
  @DisplayName("Page bị HIDE khi câu hỏi nguồn thỏa mãn điều kiện -> Câu hỏi trong Page tự động không khả dụng")
  void testPageHideConditionPropagatesToQuestions() {
    Page page1 = new Page();
    page1.setId(UUID.randomUUID());

    Page page2 = new Page();
    page2.setId(UUID.randomUUID());

    QuestionType singleChoiceType = new QuestionType();
    singleChoiceType.setCode("SINGLE_CHOICE");

    Question q1 = new Question();
    q1.setId(UUID.randomUUID());
    q1.setPage(page1);
    q1.setQuestionType(singleChoiceType);

    Question q2 = new Question();
    q2.setId(UUID.randomUUID());
    q2.setPage(page2);

    UUID optionA = UUID.randomUUID();
    Option optA = new Option();
    optA.setId(optionA);
    optA.setQuestion(q1);

    // Condition: Nếu q1 chọn optionA thì HIDE page2
    Condition pageCondition = new Condition();
    pageCondition.setId(UUID.randomUUID());
    pageCondition.setSourceQuestion(q1);
    pageCondition.setSourceValue(optionA.toString());
    pageCondition.setTargetType("PAGE");
    pageCondition.setTargetPage(page2);
    pageCondition.setAction("HIDE");

    // Người dùng chọn optionA cho q1
    Answer answer1 = new Answer();
    answer1.setQuestion(q1);
    AnswerOption ansOpt = new AnswerOption();
    ansOpt.setOption(optA);
    answer1.setAnswerOptions(List.of(ansOpt));

    Map<UUID, Answer> answerMap = Map.of(q1.getId(), answer1);
    List<Condition> conditions = List.of(pageCondition);

    // Page 2 phải bị ẩn
    boolean page2Applicable = ConditionUtil.isPageApplicable(page2, answerMap, conditions);
    assertFalse(page2Applicable, "Page 2 phải không khả dụng (bị ẩn)");

    // Câu hỏi q2 trong Page 2 phải tự động không khả dụng
    Map<UUID, Boolean> applicability = ConditionUtil.calculateApplicability(
            List.of(q1, q2),
            List.of(answer1),
            conditions
    );

    assertTrue(applicability.get(q1.getId()), "q1 phải hiển thị bình thường");
    assertFalse(applicability.get(q2.getId()), "q2 nằm trong Page 2 bị ẩn nên phải không khả dụng");
  }

  @Test
  @DisplayName("Page có SHOW condition: Chỉ hiển thị khi câu hỏi nguồn thỏa mãn")
  void testPageShowCondition() {
    Page page1 = new Page();
    page1.setId(UUID.randomUUID());

    Page page2 = new Page();
    page2.setId(UUID.randomUUID());

    QuestionType textType = new QuestionType();
    textType.setCode("TEXT");

    Question q1 = new Question();
    q1.setId(UUID.randomUUID());
    q1.setPage(page1);
    q1.setQuestionType(textType);

    Question q2 = new Question();
    q2.setId(UUID.randomUUID());
    q2.setPage(page2);

    Condition pageCondition = new Condition();
    pageCondition.setId(UUID.randomUUID());
    pageCondition.setSourceQuestion(q1);
    pageCondition.setSourceValue("YES");
    pageCondition.setTargetType("PAGE");
    pageCondition.setTargetPage(page2);
    pageCondition.setAction("SHOW");

    List<Condition> conditions = List.of(pageCondition);

    // TH1: q1 trả lời "NO" -> Page 2 không hiện, q2 không khả dụng
    Answer answerNo = new Answer();
    answerNo.setQuestion(q1);
    answerNo.setAnswerText("NO");

    Map<UUID, Boolean> appNo = ConditionUtil.calculateApplicability(
            List.of(q1, q2),
            List.of(answerNo),
            conditions
    );
    assertFalse(appNo.get(q2.getId()), "Khi chưa match SHOW, q2 phải không khả dụng");

    // TH2: q1 trả lời "YES" -> Page 2 hiện, q2 khả dụng
    Answer answerYes = new Answer();
    answerYes.setQuestion(q1);
    answerYes.setAnswerText("YES");

    Map<UUID, Boolean> appYes = ConditionUtil.calculateApplicability(
            List.of(q1, q2),
            List.of(answerYes),
            conditions
    );
    assertTrue(appYes.get(q2.getId()), "Khi match SHOW, q2 phải khả dụng");
  }
}

