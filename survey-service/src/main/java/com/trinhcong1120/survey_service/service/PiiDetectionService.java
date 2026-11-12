package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.report.SurveyDatasetResponse;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.entity.QuestionValidationRule;
import com.trinhcong1120.survey_service.repository.QuestionValidationRuleRepository;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PiiDetectionService {

  public enum PiiKind {
    PHONE,
    EMAIL,
    ID_NUMBER,
    NAME,
    ADDRESS_DETAIL
  }

  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

  private final QuestionValidationRuleRepository validationRuleRepository;

  public PiiDetectionService(QuestionValidationRuleRepository validationRuleRepository) {
    this.validationRuleRepository = validationRuleRepository;
  }

  public Map<UUID, PiiKind> detectPiiQuestions(UUID surveyId, List<Question> questions) {
    Map<UUID, PiiKind> result = new LinkedHashMap<>();
    if (questions == null || questions.isEmpty()) {
      return result;
    }

    List<QuestionValidationRule> rules =
        surveyId == null
            ? List.of()
            : validationRuleRepository.findByQuestion_Page_Survey_IdAndIsActiveTrue(surveyId);

    for (QuestionValidationRule rule : rules) {
      if (rule.getQuestion() == null || rule.getQuestion().getId() == null) continue;
      String code = rule.getRuleCode();
      if ("PHONE".equalsIgnoreCase(code)) {
        result.put(rule.getQuestion().getId(), PiiKind.PHONE);
      } else if ("EMAIL".equalsIgnoreCase(code)) {
        result.put(rule.getQuestion().getId(), PiiKind.EMAIL);
      }
    }

    for (Question question : questions) {
      if (question == null || question.getId() == null || result.containsKey(question.getId())) {
        continue;
      }
      PiiKind detected = detectFromMetadata(question);
      if (detected != null) {
        result.put(question.getId(), detected);
      }
    }
    return result;
  }

  public PiiKind detectFromMetadata(Question question) {
    if (question == null) return null;
    String normalized = normalize(question.getQuestionText());
    String typeCode =
        question.getQuestionType() != null && question.getQuestionType().getCode() != null
            ? question.getQuestionType().getCode().trim().toUpperCase(Locale.ROOT)
            : "";

    // Cho phép thống kê theo Tỉnh/Phường ở dạng ADDRESS nhưng ẩn địa chỉ chi tiết nhạy cảm
    if ("ADDRESS".equals(typeCode)) {
      return PiiKind.ADDRESS_DETAIL;
    }
    if ("NUMBER".equals(typeCode) && !normalized.contains("cccd") && !normalized.contains("cmnd")) {
      return null;
    }

    if (containsAny(normalized, "so dien thoai", "dien thoai", "sdt", "hotline", "mobile", "phone")) {
      return PiiKind.PHONE;
    }
    if (containsAny(normalized, "email", "e-mail", "thu dien tu")) {
      return PiiKind.EMAIL;
    }
    if (containsAny(normalized, "cccd", "cmnd", "can cuoc", "chung minh", "ho chieu", "passport", "ma so thue")) {
      return PiiKind.ID_NUMBER;
    }
    if (containsAny(normalized, "ho va ten", "ho ten", "full name", "nguoi nop", "nguoi lien he")) {
      return PiiKind.NAME;
    }
    return null;
  }

  public void maskAnswerValue(PiiKind kind, SurveyDatasetResponse.AnswerValue av) {
    if (kind == null || av == null) return;

    if (kind == PiiKind.ADDRESS_DETAIL) {
      if (av.getAddressDetail() != null && !av.getAddressDetail().isBlank()) {
        av.setAddressDetail("***");
      }
      if (av.getText() != null && !av.getText().isBlank()) {
        av.setText("***");
      }
      String province = av.getProvince() == null ? "" : av.getProvince();
      String ward = av.getWard() == null ? "" : av.getWard();
      if (!province.isBlank() || !ward.isBlank()) {
        av.setFormatted((province + " / " + ward).trim());
      } else if (av.getFormatted() != null && !av.getFormatted().isBlank()) {
        av.setFormatted("***");
      }
      return;
    }

    String raw = firstNonBlank(av.getText(), av.getFormatted());
    if (raw == null) return;
    String masked = maskString(kind, raw);
    av.setText(masked);
    av.setFormatted(masked);
  }

  public static String maskString(PiiKind kind, String raw) {
    if (raw == null || raw.isBlank()) return raw;
    String trimmed = raw.trim();
    return switch (kind) {
      case PHONE -> {
        String compact = trimmed.replaceAll("\\s+", "");
        if (compact.length() <= 4) yield "***";
        if (compact.length() <= 6) yield compact.substring(0, 2) + "***";
        yield compact.substring(0, 3) + "***" + compact.substring(compact.length() - 2);
      }
      case EMAIL -> {
        int at = trimmed.indexOf('@');
        if (at <= 0) yield "***";
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        String prefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        yield prefix + "***" + domain;
      }
      case ID_NUMBER -> {
        if (trimmed.length() <= 4) yield "***";
        yield "***" + trimmed.substring(trimmed.length() - 4);
      }
      case NAME -> {
        String[] parts = trimmed.split("\\s+");
        if (parts.length <= 1) {
          yield parts[0].length() <= 1 ? "***" : parts[0].charAt(0) + "***";
        }
        yield parts[0] + " ***";
      }
      case ADDRESS_DETAIL -> "***";
    };
  }

  private static String normalize(String input) {
    if (input == null) return "";
    String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
    String stripped = COMBINING_MARKS.matcher(decomposed).replaceAll("");
    return stripped
        .replace('\u0111', 'd')
        .replace('\u0110', 'D')
        .toLowerCase(Locale.ROOT)
        .trim();
  }

  private static boolean containsAny(String haystack, String... needles) {
    for (String needle : needles) {
      if (haystack.contains(needle)) return true;
    }
    return false;
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return null;
  }
}

