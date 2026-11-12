package com.trinhcong1120.survey_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.submit.SubmitAnswerRequest;
import com.trinhcong1120.survey_service.entity.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationEngineServiceTest {

  private AggregationEngineService aggregationEngineService;

  @BeforeEach
  void setUp() {
    aggregationEngineService = new AggregationEngineService(new ObjectMapper());
  }

  @Test
  @DisplayName("Kiểm tra tính tổng SUM từ 2 tiêu chí con lên 1 tiêu chí cha")
  void testSumAggregation() {
    UUID parentId = UUID.randomUUID();
    Question parent = new Question();
    parent.setId(parentId);
    parent.setLevel(1);
    parent.setAggregationType("SUM");

    UUID child1Id = UUID.randomUUID();
    Question child1 = new Question();
    child1.setId(child1Id);
    child1.setParent(parent);
    child1.setLevel(2);

    UUID child2Id = UUID.randomUUID();
    Question child2 = new Question();
    child2.setId(child2Id);
    child2.setParent(parent);
    child2.setLevel(2);

    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    SubmitAnswerRequest ans1 = new SubmitAnswerRequest();
    ans1.setQuestionId(child1Id);
    ans1.setAnswerNumber(new BigDecimal("150.50"));
    answerMap.put(child1Id, ans1);

    SubmitAnswerRequest ans2 = new SubmitAnswerRequest();
    ans2.setQuestionId(child2Id);
    ans2.setAnswerNumber(new BigDecimal("49.50"));
    answerMap.put(child2Id, ans2);

    aggregationEngineService.calculateAggregations(List.of(parent, child1, child2), answerMap);

    SubmitAnswerRequest parentAnswer = answerMap.get(parentId);
    assertNotNull(parentAnswer, "Câu trả lời của cha phải được tự động sinh ra");
    assertEquals(new BigDecimal("200.00"), parentAnswer.getAnswerNumber());
    assertEquals("200", parentAnswer.getAnswerText());
  }

  @Test
  @DisplayName("Kiểm tra tính trung bình cộng AVG từ các con lên cha")
  void testAvgAggregation() {
    UUID parentId = UUID.randomUUID();
    Question parent = new Question();
    parent.setId(parentId);
    parent.setLevel(1);
    parent.setAggregationType("AVG");

    UUID c1 = UUID.randomUUID();
    Question child1 = new Question();
    child1.setId(c1);
    child1.setParent(parent);
    child1.setLevel(2);

    UUID c2 = UUID.randomUUID();
    Question child2 = new Question();
    child2.setId(c2);
    child2.setParent(parent);
    child2.setLevel(2);

    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    SubmitAnswerRequest ans1 = new SubmitAnswerRequest();
    ans1.setQuestionId(c1);
    ans1.setAnswerNumber(new BigDecimal("8.00"));
    answerMap.put(c1, ans1);

    SubmitAnswerRequest ans2 = new SubmitAnswerRequest();
    ans2.setQuestionId(c2);
    ans2.setAnswerNumber(new BigDecimal("10.00"));
    answerMap.put(c2, ans2);

    aggregationEngineService.calculateAggregations(List.of(parent, child1, child2), answerMap);

    SubmitAnswerRequest parentAnswer = answerMap.get(parentId);
    assertNotNull(parentAnswer);
    assertEquals(new BigDecimal("9.00"), parentAnswer.getAnswerNumber());
  }

  @Test
  @DisplayName("Kiểm tra tính trung bình có trọng số WEIGHTED")
  void testWeightedAggregation() {
    UUID parentId = UUID.randomUUID();
    Question parent = new Question();
    parent.setId(parentId);
    parent.setLevel(1);
    parent.setAggregationType("WEIGHTED");

    UUID c1 = UUID.randomUUID();
    Question child1 = new Question();
    child1.setId(c1);
    child1.setParent(parent);
    child1.setLevel(2);
    child1.setWeight(new BigDecimal("2.0")); // Trọng số 2

    UUID c2 = UUID.randomUUID();
    Question child2 = new Question();
    child2.setId(c2);
    child2.setParent(parent);
    child2.setLevel(2);
    child2.setWeight(new BigDecimal("1.0")); // Trọng số 1

    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    SubmitAnswerRequest ans1 = new SubmitAnswerRequest();
    ans1.setQuestionId(c1);
    ans1.setAnswerNumber(new BigDecimal("10.00")); // 10 * 2 = 20
    answerMap.put(c1, ans1);

    SubmitAnswerRequest ans2 = new SubmitAnswerRequest();
    ans2.setQuestionId(c2);
    ans2.setAnswerNumber(new BigDecimal("4.00")); // 4 * 1 = 4
    answerMap.put(c2, ans2);
    // (20 + 4) / (2 + 1) = 24 / 3 = 8.00

    aggregationEngineService.calculateAggregations(List.of(parent, child1, child2), answerMap);

    SubmitAnswerRequest parentAnswer = answerMap.get(parentId);
    assertNotNull(parentAnswer);
    assertEquals(new BigDecimal("8.00"), parentAnswer.getAnswerNumber());
  }

  @Test
  @DisplayName("Kiểm tra biểu thức tùy biến CUSTOM_FORMULA: [Q1] * 0.4 + [Q2] * 0.6")
  void testCustomFormulaAggregation() {
    UUID parentId = UUID.randomUUID();
    Question parent = new Question();
    parent.setId(parentId);
    parent.setLevel(1);
    parent.setAggregationType("CUSTOM_FORMULA");
    parent.setAggregationFormula("[Q1] * 0.4 + [Q2] * 0.6");

    UUID c1 = UUID.randomUUID();
    Question child1 = new Question();
    child1.setId(c1);
    child1.setOrderIndex(1);
    child1.setParent(parent);
    child1.setLevel(2);

    UUID c2 = UUID.randomUUID();
    Question child2 = new Question();
    child2.setId(c2);
    child2.setOrderIndex(2);
    child2.setParent(parent);
    child2.setLevel(2);

    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    SubmitAnswerRequest ans1 = new SubmitAnswerRequest();
    ans1.setQuestionId(c1);
    ans1.setAnswerNumber(new BigDecimal("100"));
    answerMap.put(c1, ans1);

    SubmitAnswerRequest ans2 = new SubmitAnswerRequest();
    ans2.setQuestionId(c2);
    ans2.setAnswerNumber(new BigDecimal("50"));
    answerMap.put(c2, ans2);
    // 100 * 0.4 + 50 * 0.6 = 40 + 30 = 70.00

    aggregationEngineService.calculateAggregations(List.of(parent, child1, child2), answerMap);

    SubmitAnswerRequest parentAnswer = answerMap.get(parentId);
    assertNotNull(parentAnswer);
    assertEquals(new BigDecimal("70.00"), parentAnswer.getAnswerNumber());
  }

  @Test
  @DisplayName("Kiểm tra tổng hợp đa cấp 3 tầng: Con -> Cha -> Ông")
  void testMultiLevelCascadeAggregation() {
    // Tầng 1: Ông (SUM)
    UUID grandParentId = UUID.randomUUID();
    Question grandParent = new Question();
    grandParent.setId(grandParentId);
    grandParent.setLevel(1);
    grandParent.setAggregationType("SUM");

    // Tầng 2: Cha 1 (SUM)
    UUID parent1Id = UUID.randomUUID();
    Question parent1 = new Question();
    parent1.setId(parent1Id);
    parent1.setParent(grandParent);
    parent1.setLevel(2);
    parent1.setAggregationType("SUM");

    // Tầng 3: Con 1.1 và Con 1.2
    UUID c11 = UUID.randomUUID();
    Question child11 = new Question();
    child11.setId(c11);
    child11.setParent(parent1);
    child11.setLevel(3);

    UUID c12 = UUID.randomUUID();
    Question child12 = new Question();
    child12.setId(c12);
    child12.setParent(parent1);
    child12.setLevel(3);

    // Tầng 2: Cha 2 (nhập trực tiếp hoặc có con riêng)
    UUID parent2Id = UUID.randomUUID();
    Question parent2 = new Question();
    parent2.setId(parent2Id);
    parent2.setParent(grandParent);
    parent2.setLevel(2);

    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    SubmitAnswerRequest a11 = new SubmitAnswerRequest();
    a11.setQuestionId(c11);
    a11.setAnswerNumber(new BigDecimal("10.00"));
    answerMap.put(c11, a11);

    SubmitAnswerRequest a12 = new SubmitAnswerRequest();
    a12.setQuestionId(c12);
    a12.setAnswerNumber(new BigDecimal("20.00"));
    answerMap.put(c12, a12);

    SubmitAnswerRequest a2 = new SubmitAnswerRequest();
    a2.setQuestionId(parent2Id);
    a2.setAnswerNumber(new BigDecimal("70.00"));
    answerMap.put(parent2Id, a2);

    // Run cascade
    aggregationEngineService.calculateAggregations(
            List.of(grandParent, parent1, parent2, child11, child12),
            answerMap
    );

    // Cha 1 = 10 + 20 = 30
    assertEquals(new BigDecimal("30.00"), answerMap.get(parent1Id).getAnswerNumber());
    // Ông = Cha 1 (30) + Cha 2 (70) = 100
    assertEquals(new BigDecimal("100.00"), answerMap.get(grandParentId).getAnswerNumber());
  }

  @Test
  @DisplayName("Kiểm tra tổng hợp số liệu từ câu hỏi Bảng/Ma trận (matrixData)")
  void testMatrixDataAggregation() {
    UUID parentId = UUID.randomUUID();
    Question parent = new Question();
    parent.setId(parentId);
    parent.setLevel(1);
    parent.setAggregationType("SUM");

    UUID matrixQId = UUID.randomUUID();
    Question matrixQ = new Question();
    matrixQ.setId(matrixQId);
    matrixQ.setParent(parent);
    matrixQ.setLevel(2);

    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    // Dữ liệu bảng động với 2 dòng
    String matrixJson = """
            [
              {"col1": "Nguyen Van A", "col_amount": 500000},
              {"col1": "Tran Thi B", "col_amount": 350000}
            ]
            """;
    SubmitAnswerRequest matrixAns = new SubmitAnswerRequest();
    matrixAns.setQuestionId(matrixQId);
    matrixAns.setMatrixData(matrixJson);
    answerMap.put(matrixQId, matrixAns);

    aggregationEngineService.calculateAggregations(List.of(parent, matrixQ), answerMap);

    SubmitAnswerRequest parentAns = answerMap.get(parentId);
    assertNotNull(parentAns);
    // Tổng = 500.000 + 350.000 = 850.000
    assertEquals(new BigDecimal("850000.00"), parentAns.getAnswerNumber());
  }
}

