package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import com.trinhcong1120.survey_service.service.DashboardReportingService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/Dashboard")
@PreAuthorize("hasAuthority('dashboard_view') or hasAuthority('survey_view')")
public class DashboardReportingController {
  private final DashboardReportingService reporting;
  private final SurveyAccessGuard guard;

  public DashboardReportingController(DashboardReportingService reporting, SurveyAccessGuard guard) {
    this.reporting = reporting;
    this.guard = guard;
  }

  private record ScopeInfo(boolean isScoped, List<UUID> ids, List<Survey> surveys) {}

  private ScopeInfo resolveScope(String scope) {
    boolean canSeeAll = guard.has("survey_view_all");
    boolean wantAll = "all".equalsIgnoreCase(scope);
    if (canSeeAll && (wantAll || "auto".equalsIgnoreCase(scope))) {
      return new ScopeInfo(false, null, null);
    }
    List<Survey> visible = guard.visibleSurveys();
    List<UUID> ids = visible.stream().map(Survey::getId).toList();
    return new ScopeInfo(true, ids, visible);
  }

  @GetMapping("/report")
  public Map<String,Object> report(@RequestParam(required=false) Instant from,
      @RequestParam(required=false) Instant to, @RequestParam(defaultValue="AUTO") String bucket,
      @RequestParam(required=false, defaultValue="auto") String scope) {
    ScopeInfo s = resolveScope(scope);
    return reporting.report(reporting.period(from,to,bucket), s.ids(), s.surveys());
  }

  @GetMapping("/surveys")
  public Map<String,Object> surveys(@RequestParam(required=false) Instant from,
      @RequestParam(required=false) Instant to, @RequestParam(defaultValue="AUTO") String bucket,
      @RequestParam(required=false) String search, @RequestParam(required=false) String status,
      @RequestParam(defaultValue="submissions") String sort,
      @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size,
      @RequestParam(required=false, defaultValue="auto") String scope) {
    ScopeInfo s = resolveScope(scope);
    return reporting.surveyRows(reporting.period(from,to,bucket),search,status,sort,page,size, s.ids(), s.surveys());
  }

  @GetMapping("/surveys.xlsx")
  public ResponseEntity<byte[]> export(@RequestParam(required=false) Instant from,
      @RequestParam(required=false) Instant to, @RequestParam(defaultValue="AUTO") String bucket,
      @RequestParam(required=false) String search, @RequestParam(required=false) String status,
      @RequestParam(defaultValue="submissions") String sort,
      @RequestParam(required=false, defaultValue="auto") String scope) throws IOException {
    ScopeInfo s = resolveScope(scope);
    var period = reporting.period(from,to,bucket);
    try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("So sanh khao sat");
      String[] headers = {"Khảo sát","Trạng thái","Lượt xem","Phiên","Phản hồi",
          "Chuyển đổi %","Câu áp dụng chính xác","Câu trả lời chính xác",
          "Câu bỏ qua chính xác","Đầy đủ chính xác %","Phản hồi có bộ đếm câu",
          "Phản hồi cũ thiếu bộ đếm","Thời gian tương tác trung vị (ms)","Tracking",
          "Nguồn đếm câu"};
      Row head = sheet.createRow(0);
      for (int i=0;i<headers.length;i++) head.createCell(i).setCellValue(headers[i]);
      int rowNumber = 1, page = 1;
      while (true) {
        Map<String,Object> batch = reporting.surveyRows(period,search,status,sort,page++,100, s.ids(), s.surveys());
        @SuppressWarnings("unchecked") List<Map<String,Object>> rows = (List<Map<String,Object>>) batch.get("rows");
        for (Map<String,Object> data : rows) {
          Row line = sheet.createRow(rowNumber++);
          String[] keys = {"title","status","views","sessions","submissions","conversionRate",
              "applicableQuestions","answeredQuestions","skippedQuestions","completionRate",
              "questionCountResponses","estimatedLegacyResponses","medianActiveDurationMs",
              "trackingStatus","questionCountSource"};
          for (int i=0;i<keys.length;i++) {
            Object value = data.get(keys[i]);
            if ("questionCountSource".equals(keys[i])) line.createCell(i).setCellValue(
                "EXACT".equals(value) ? "Chính xác" : "PARTIAL".equals(value)
                    ? "Một phần chính xác" : "Chưa có bộ đếm");
            else if (value instanceof Number number) line.createCell(i).setCellValue(number.doubleValue());
            else line.createCell(i).setCellValue(value == null ? "—" : value.toString());
          }
        }
        if (rows.isEmpty() || rowNumber - 1 >= ((Number)batch.get("total")).intValue()) break;
      }
      workbook.write(out);
      byte[] bytes = out.toByteArray();
      return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=\"survey-dashboard.xlsx\"")
          .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .contentLength(bytes.length).body(bytes);
    }
  }
}
