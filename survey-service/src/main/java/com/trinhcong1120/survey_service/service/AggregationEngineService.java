package com.trinhcong1120.survey_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.submit.SubmitAnswerRequest;
import com.trinhcong1120.survey_service.entity.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AggregationEngineService {

  private static final Logger log = LoggerFactory.getLogger(AggregationEngineService.class);
  private static final Pattern FORMULA_VAR_PATTERN = Pattern.compile("\\[([a-zA-Z0-9_-]+)\\]");

  private final ObjectMapper objectMapper;
  private final ExpressionParser expressionParser = new SpelExpressionParser();

  public AggregationEngineService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Tính toán và tự động cập nhật giá trị cho các tiêu chí Cha theo cơ chế Bottom-Up.
   *
   * @param questions Danh sách toàn bộ câu hỏi/tiêu chí trong phạm vi khảo sát
   * @param answerMap Bản đồ lưu câu trả lời theo questionId
   */
  public void calculateAggregations(List<Question> questions, Map<UUID, SubmitAnswerRequest> answerMap) {
    if (questions == null || questions.isEmpty() || answerMap == null) {
      return;
    }

    Map<UUID, Question> questionById = new HashMap<>();
    for (Question q : questions) {
      if (q != null && q.getId() != null) {
        questionById.put(q.getId(), q);
      }
    }

    // Lọc các câu hỏi cha có cấu hình tổng hợp số liệu
    List<Question> parentQuestions = questions.stream()
            .filter(q -> q != null && q.getId() != null)
            .filter(q -> q.getAggregationType() != null && !q.getAggregationType().equalsIgnoreCase("NONE"))
            .sorted(Comparator.comparingInt(Question::getLevel).reversed()) // Duyệt từ cấp sâu nhất lên gốc
            .toList();

    for (Question parent : parentQuestions) {
      UUID parentId = parent.getId();
      List<Question> directChildren = questions.stream()
              .filter(q -> q != null && q.getParent() != null && parentId.equals(q.getParent().getId()))
              .toList();

      BigDecimal calculatedValue = calculateForParent(parent, directChildren, answerMap);

      if (calculatedValue != null) {
        SubmitAnswerRequest parentAnswer = answerMap.computeIfAbsent(parentId, id -> {
          SubmitAnswerRequest req = new SubmitAnswerRequest();
          req.setQuestionId(id);
          return req;
        });
        parentAnswer.setAnswerNumber(calculatedValue.setScale(2, RoundingMode.HALF_UP));
        parentAnswer.setAnswerText(calculatedValue.stripTrailingZeros().toPlainString());
      }
    }
  }

  private BigDecimal calculateForParent(Question parent, List<Question> children, Map<UUID, SubmitAnswerRequest> answerMap) {
    String aggType = parent.getAggregationType() != null ? parent.getAggregationType().trim().toUpperCase() : "NONE";

    return switch (aggType) {
      case "SUM" -> calculateSum(children, answerMap);
      case "AVG" -> calculateAvg(children, answerMap);
      case "WEIGHTED" -> calculateWeighted(children, answerMap);
      case "CUSTOM_FORMULA" -> calculateCustomFormula(parent, children, answerMap);
      default -> null;
    };
  }

  private BigDecimal calculateSum(List<Question> children, Map<UUID, SubmitAnswerRequest> answerMap) {
    BigDecimal total = BigDecimal.ZERO;
    boolean hasValue = false;

    for (Question child : children) {
      BigDecimal val = extractNumericValue(child, answerMap.get(child.getId()));
      if (val != null) {
        total = total.add(val);
        hasValue = true;
      }
    }
    return hasValue ? total : null;
  }

  private BigDecimal calculateAvg(List<Question> children, Map<UUID, SubmitAnswerRequest> answerMap) {
    BigDecimal total = BigDecimal.ZERO;
    int count = 0;

    for (Question child : children) {
      BigDecimal val = extractNumericValue(child, answerMap.get(child.getId()));
      if (val != null) {
        total = total.add(val);
        count++;
      }
    }
    if (count == 0) return null;
    return total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateWeighted(List<Question> children, Map<UUID, SubmitAnswerRequest> answerMap) {
    BigDecimal weightedSum = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;

    for (Question child : children) {
      BigDecimal val = extractNumericValue(child, answerMap.get(child.getId()));
      if (val != null) {
        BigDecimal weight = child.getWeight() != null ? child.getWeight() : BigDecimal.ONE;
        weightedSum = weightedSum.add(val.multiply(weight));
        totalWeight = totalWeight.add(weight);
      }
    }
    if (totalWeight.compareTo(BigDecimal.ZERO) == 0) return null;
    return weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateCustomFormula(Question parent, List<Question> children, Map<UUID, SubmitAnswerRequest> answerMap) {
    String formula = parent.getAggregationFormula();
    if (formula == null || formula.isBlank()) {
      return calculateSum(children, answerMap);
    }

    try {
      // Chuẩn bị biến theo ID hoặc thứ tự orderIndex
      Map<String, BigDecimal> variables = new HashMap<>();
      for (Question child : children) {
        BigDecimal val = extractNumericValue(child, answerMap.get(child.getId()));
        BigDecimal num = val != null ? val : BigDecimal.ZERO;
        variables.put(child.getId().toString(), num);
        if (child.getOrderIndex() != null) {
          variables.put("Q" + child.getOrderIndex(), num);
          variables.put("q" + child.getOrderIndex(), num);
        }
      }

      // Thay thế các biến dạng [var] thành giá trị số
      Matcher matcher = FORMULA_VAR_PATTERN.matcher(formula);
      StringBuilder exprBuilder = new StringBuilder();
      while (matcher.find()) {
        String varName = matcher.group(1);
        BigDecimal val = variables.getOrDefault(varName, BigDecimal.ZERO);
        matcher.appendReplacement(exprBuilder, val.toPlainString());
      }
      matcher.appendTail(exprBuilder);

      String parsedExpression = exprBuilder.toString();
      Expression expr = expressionParser.parseExpression(parsedExpression);
      StandardEvaluationContext context = new StandardEvaluationContext();
      Object evaluated = expr.getValue(context);

      if (evaluated instanceof Number num) {
        return new BigDecimal(num.toString());
      }
    } catch (Exception e) {
      log.warn("Không thể tính công thức tùy biến: '{}' cho Question ID: {}. Lỗi: {}", formula, parent.getId(), e.getMessage());
    }
    return null;
  }

  /**
   * Trích xuất giá trị số từ câu trả lời của câu hỏi (hỗ trợ cả câu hỏi số thông thường và bảng/ma trận)
   */
  public BigDecimal extractNumericValue(Question question, SubmitAnswerRequest answer) {
    if (answer == null) {
      return null;
    }
    // Nếu có giá trị số trực tiếp
    if (answer.getAnswerNumber() != null) {
      return answer.getAnswerNumber();
    }

    // Nếu là câu hỏi ma trận hoặc bảng động, trích xuất tổng số liệu trong bảng
    if (answer.getMatrixData() != null && !answer.getMatrixData().isBlank()) {
      return sumMatrixValues(answer.getMatrixData());
    }

    // Nếu có chuỗi văn bản nhưng là số
    if (answer.getAnswerText() != null && !answer.getAnswerText().isBlank()) {
      try {
        return new BigDecimal(answer.getAnswerText().trim());
      } catch (NumberFormatException ignored) {
      }
    }

    return null;
  }

  private BigDecimal sumMatrixValues(String matrixJson) {
    try {
      JsonNode root = objectMapper.readTree(matrixJson);
      BigDecimal sum = BigDecimal.ZERO;
      boolean foundNumber = false;

      if (root.isArray()) {
        for (JsonNode row : root) {
          if (row.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
            while (fields.hasNext()) {
              JsonNode cell = fields.next().getValue();
              if (cell.isNumber()) {
                sum = sum.add(new BigDecimal(cell.asText()));
                foundNumber = true;
              } else if (cell.isTextual()) {
                try {
                  sum = sum.add(new BigDecimal(cell.asText().trim()));
                  foundNumber = true;
                } catch (NumberFormatException ignored) {
                }
              }
            }
          } else if (row.isNumber()) {
            sum = sum.add(new BigDecimal(row.asText()));
            foundNumber = true;
          }
        }
      }
      return foundNumber ? sum : null;
    } catch (Exception e) {
      log.warn("Không thể phân tích dữ liệu matrix json: {}", matrixJson);
      return null;
    }
  }
}

