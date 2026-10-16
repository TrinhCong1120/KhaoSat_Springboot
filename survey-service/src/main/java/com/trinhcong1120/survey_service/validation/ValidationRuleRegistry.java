package com.trinhcong1120.survey_service.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ValidationRuleRegistry {

  private final Map<String, RuleDefinition> definitions = new LinkedHashMap<>();

  public ValidationRuleRegistry() {
    registerNumberRules();
    registerTextRules();
    registerChoiceRules();
    registerDateRules();
    registerAddressRules();
  }

  public RuleDefinition getRequired(String code) {
    RuleDefinition definition = definitions.get(normalize(code));

    if (definition == null) {
      throw new IllegalArgumentException("Unsupported validation rule: " + code);
    }

    return definition;
  }

  public RuleDefinition get(String code) {
    return definitions.get(normalize(code));
  }

  public Collection<RuleDefinition> all() {
    return definitions.values();
  }

  public List<RuleDefinition> byQuestionType(String questionType) {
    if (questionType == null || questionType.isBlank()) {
      return new ArrayList<>(definitions.values());
    }

    String normalized = questionType.trim().toUpperCase();

    return definitions.values()
            .stream()
            .filter(definition -> definition.appliesTo(normalized))
            .toList();
  }

  public String normalize(String code) {
    return code == null ? "" : code.trim().toUpperCase();
  }

  private void put(RuleDefinition definition) {
    definitions.put(definition.code(), definition);
  }

  private void registerNumberRules() {
    Set<String> types = Set.of("NUMBER");
    put(rule("MIN", "Gia tri nho nhat", types, number("value"), "Gia tri qua nho"));
    put(rule("MAX", "Gia tri lon nhat", types, number("value"), "Gia tri qua lon"));
    put(rule("GREATER_THAN", "Lon hon", types, number("value"), "Gia tri phai lon hon gioi han"));
    put(rule("LESS_THAN", "Nho hon", types, number("value"), "Gia tri phai nho hon gioi han"));
    put(rule(
            "STEP",
            "Buoc gia tri",
            types,
            List.of(
                    ParameterSpec.required("step", ParameterType.NUMBER),
                    ParameterSpec.optional("base", ParameterType.NUMBER, "0")
            ),
            "NUMBER_STEP",
            "Gia tri khong dung buoc quy dinh"
    ));
    put(rule("ALLOWED_VALUES", "Gia tri duoc phep", types, numberList("value"), "Gia tri khong nam trong danh sach cho phep"));
    put(rule("DISALLOWED_VALUES", "Gia tri bi cam", types, numberList("value"), "Gia tri khong duoc phep"));
  }

  private void registerTextRules() {
    Set<String> types = Set.of("TEXT");
    put(rule("MIN_LENGTH", "Do dai toi thieu", types, number("value"), "Noi dung qua ngan"));
    put(rule("MAX_LENGTH", "Do dai toi da", types, number("value"), "Noi dung qua dai"));
    put(rule("REGEX", "Bieu thuc chinh quy", types, text("value"), "Noi dung khong dung dinh dang"));
    put(rule("EMAIL", "Email", types, List.of(), "Email khong dung dinh dang"));
    put(rule("PHONE", "So dien thoai", types, text("country"), "So dien thoai khong dung dinh dang"));
    put(rule("URL", "URL", types, List.of(), "URL khong dung dinh dang"));
    put(rule("CHARACTER_SET", "Bo ky tu", types, text("value"), "Noi dung chua ky tu khong hop le"));
    put(rule("DIACRITICS", "Dau tieng Viet", types, text("value"), "Noi dung khong dung quy tac dau"));
    put(rule("WHITESPACE", "Khoang trang", types, text("value"), "Noi dung khong dung quy tac khoang trang"));
    put(rule("STARTS_WITH", "Bat dau bang", types, text("value"), "Noi dung khong dung tien to"));
    put(rule("ENDS_WITH", "Ket thuc bang", types, text("value"), "Noi dung khong dung hau to"));
    put(rule("MUST_CONTAIN", "Phai chua", types, textList("value"), "Noi dung thieu cum bat buoc"));
    put(rule("NOT_CONTAIN", "Khong duoc chua", types, textList("value"), "Noi dung chua cum bi cam"));
  }

  private void registerChoiceRules() {
    put(rule("FIXED_OPTION", "Co dinh lua chon", Set.of("SINGLE_CHOICE"), option("value"), "Lua chon khong dung cau hinh"));
    put(rule("ALLOWED_OPTIONS", "Lua chon duoc phep", Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE"), optionList("value"), "Lua chon khong nam trong danh sach cho phep"));
    put(rule("DISALLOWED_OPTIONS", "Lua chon bi cam", Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE"), optionList("value"), "Lua chon khong duoc phep"));
    put(rule("MIN_SELECTIONS", "So lua chon toi thieu", Set.of("MULTIPLE_CHOICE"), number("value"), "Chon qua it phuong an"));
    put(rule("MAX_SELECTIONS", "So lua chon toi da", Set.of("MULTIPLE_CHOICE"), number("value"), "Chon qua nhieu phuong an"));
    put(rule("REQUIRED_OPTIONS", "Lua chon bat buoc", Set.of("MULTIPLE_CHOICE"), optionList("value"), "Thieu lua chon bat buoc"));
    put(rule("AT_MOST_ONE_OF", "Toi da mot trong nhom", Set.of("MULTIPLE_CHOICE"), optionList("value"), "Chi duoc chon toi da mot phuong an trong nhom"));
    put(rule("EXCLUSIVE_OPTIONS", "Lua chon doc quyen", Set.of("MULTIPLE_CHOICE"), optionList("value"), "Lua chon nay khong duoc chon cung phuong an khac"));
  }

  private void registerDateRules() {
    Set<String> types = Set.of("DATE");
    put(rule("MIN_DATE", "Ngay nho nhat", types, date("value"), "Ngay qua som"));
    put(rule("MAX_DATE", "Ngay lon nhat", types, date("value"), "Ngay qua muon"));
    put(rule("BEFORE_TODAY", "Truoc hom nay", types, List.of(), "Ngay phai truoc hom nay"));
    put(rule("AFTER_TODAY", "Sau hom nay", types, List.of(), "Ngay phai sau hom nay"));
    put(rule("NOT_FUTURE", "Khong trong tuong lai", types, List.of(), "Ngay khong duoc o tuong lai"));
    put(rule("NOT_PAST", "Khong trong qua khu", types, List.of(), "Ngay khong duoc o qua khu"));
    put(rule("MIN_AGE", "Tuoi toi thieu", types, number("value"), "Chua du tuoi toi thieu"));
    put(rule("MAX_AGE", "Tuoi toi da", types, number("value"), "Vuot tuoi toi da"));
    put(rule("ALLOWED_WEEKDAYS", "Thu duoc phep", types, numberList("value"), "Thu trong tuan khong duoc phep"));
    put(rule("ALLOWED_DATES", "Ngay duoc phep", types, dateList("value"), "Ngay khong nam trong danh sach cho phep"));
    put(rule("DISALLOWED_DATES", "Ngay bi cam", types, dateList("value"), "Ngay khong duoc phep"));
  }

  private void registerAddressRules() {
    Set<String> types = Set.of("ADDRESS");
    put(rule("REQUIRE_PROVINCE", "Bat buoc tinh/thanh", types, List.of(), "Vui long chon tinh/thanh"));
    put(rule("REQUIRE_WARD", "Bat buoc xa/phuong", types, List.of(), "Vui long chon xa/phuong"));
    put(rule("REQUIRE_DETAIL", "Bat buoc dia chi chi tiet", types, List.of(), "Vui long nhap dia chi chi tiet"));
    put(rule("FIXED_PROVINCE", "Co dinh tinh/thanh", types, text("value"), "Tinh/thanh khong dung"));
    put(rule("FIXED_WARD", "Co dinh xa/phuong", types, List.of(text("province"), text("ward")), "Xa/phuong khong dung"));
    put(rule("ALLOWED_PROVINCES", "Tinh/thanh duoc phep", types, textList("value"), "Tinh/thanh khong nam trong danh sach cho phep"));
    put(rule("DISALLOWED_PROVINCES", "Tinh/thanh bi cam", types, textList("value"), "Tinh/thanh khong duoc phep"));
    put(rule("ALLOWED_WARDS", "Xa/phuong duoc phep", types, List.of(text("province"), text("ward")), "Xa/phuong khong nam trong danh sach cho phep"));
    put(rule("DISALLOWED_WARDS", "Xa/phuong bi cam", types, List.of(text("province"), text("ward")), "Xa/phuong khong duoc phep"));
    put(rule("DETAIL_MIN_LENGTH", "Do dai dia chi toi thieu", types, number("value"), "Dia chi chi tiet qua ngan"));
    put(rule("DETAIL_MAX_LENGTH", "Do dai dia chi toi da", types, number("value"), "Dia chi chi tiet qua dai"));
    put(rule("DETAIL_REGEX", "Regex dia chi chi tiet", types, text("value"), "Dia chi chi tiet khong dung dinh dang"));
  }

  private RuleDefinition rule(
          String code,
          String name,
          Set<String> types,
          ParameterSpec parameter,
          String defaultMessage
  ) {
    return rule(code, name, types, List.of(parameter), defaultMessage);
  }

  private RuleDefinition rule(
          String code,
          String name,
          Set<String> types,
          List<ParameterSpec> parameters,
          String defaultMessage
  ) {
    return rule(code, name, types, parameters, "DEFAULT", defaultMessage);
  }

  private RuleDefinition rule(
          String code,
          String name,
          Set<String> types,
          List<ParameterSpec> parameters,
          String editor,
          String defaultMessage
  ) {
    return new RuleDefinition(
            code,
            name,
            name,
            types,
            parameters,
            editor,
            defaultMessage
    );
  }

  private ParameterSpec text(String name) {
    return ParameterSpec.required(name, ParameterType.TEXT);
  }

  private List<ParameterSpec> textList(String name) {
    return List.of(ParameterSpec.list(name, ParameterType.TEXT));
  }

  private ParameterSpec number(String name) {
    return ParameterSpec.required(name, ParameterType.NUMBER);
  }

  private List<ParameterSpec> numberList(String name) {
    return List.of(ParameterSpec.list(name, ParameterType.NUMBER));
  }

  private ParameterSpec date(String name) {
    return ParameterSpec.required(name, ParameterType.DATE);
  }

  private List<ParameterSpec> dateList(String name) {
    return List.of(ParameterSpec.list(name, ParameterType.DATE));
  }

  private ParameterSpec option(String name) {
    return ParameterSpec.required(name, ParameterType.OPTION);
  }

  private List<ParameterSpec> optionList(String name) {
    return List.of(ParameterSpec.list(name, ParameterType.OPTION));
  }
}
