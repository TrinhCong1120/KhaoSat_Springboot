package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.statistics.MatrixStatisticsResponse;
import com.trinhcong1120.survey_service.dto.report.SurveyDatasetResponse;

import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {

  private final SurveyRepository surveyRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final StatisticsService statisticsService;
  private final PiiDetectionService piiDetectionService;
  private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  @org.springframework.beans.factory.annotation.Autowired
  public ReportService(
          SurveyRepository surveyRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          StatisticsService statisticsService,
          PiiDetectionService piiDetectionService,
          org.springframework.jdbc.core.JdbcTemplate jdbcTemplate
  ) {
    this.surveyRepository = surveyRepository;
    this.guard = guard;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.statisticsService = statisticsService;
    this.piiDetectionService = piiDetectionService;
    this.jdbcTemplate = jdbcTemplate;
  }

  public ReportService(
          SurveyRepository surveyRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          StatisticsService statisticsService
  ) {
    this(surveyRepository, guard, pageRepository, questionRepository,
        responseRepository, answerRepository, answerOptionRepository,
        statisticsService, null, null);
  }

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private static class ExportColumn {
    final Question question;
    final String headerText;
    final boolean isMatrixRow;
    final String rowId;
    final Map<String, String> colLabels;
    final String questionType;

    ExportColumn(Question question, String headerText, String questionType) {
      this(question, headerText, false, null, Collections.emptyMap(), questionType);
    }

    ExportColumn(
            Question question,
            String headerText,
            boolean isMatrixRow,
            String rowId,
            Map<String, String> colLabels,
            String questionType
    ) {
      this.question = question;
      this.headerText = headerText;
      this.isMatrixRow = isMatrixRow;
      this.rowId = rowId;
      this.colLabels = colLabels;
      this.questionType = questionType;
    }
  }

  public SurveyDatasetResponse getSurveyDataset(UUID surveyId) {
    return getSurveyDataset(surveyId, false);
  }

  public SurveyDatasetResponse getSurveyDataset(UUID surveyId, boolean includePii) {
    guard.viewResponses(surveyId);
    boolean canExportData = guard.can(surveyId, "EXPORT_DATA");
    if (includePii) {
      guard.exportData(surveyId);
    }

    Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new NotFoundException("Survey không tồn tại"));

    List<Question> surveyQuestions = questionRepository.findByPage_Survey_Id(surveyId);
    Map<UUID, PiiDetectionService.PiiKind> piiMap = piiDetectionService != null
            ? piiDetectionService.detectPiiQuestions(surveyId, surveyQuestions)
            : Collections.emptyMap();
    Map<UUID, Long> sessionDurations = loadSessionDurations(surveyId);

    List<Response> responses = responseRepository.findBySurvey_IdOrderBySubmittedAtDesc(surveyId);
    List<SurveyDatasetResponse.RecordItem> records = new ArrayList<>();

    for (Response response : responses) {
      List<Answer> answers = answerRepository.findByResponse_Id(response.getId());
      Map<String, SurveyDatasetResponse.AnswerValue> answerMap = new HashMap<>();
      double numericSum = 0.0;
      boolean hasRootNumeric = false;

      for (Answer a : answers) {
        Question question = a.getQuestion();
        if (question == null || question.getId() == null) continue;
        UUID questionUuid = question.getId();
        String qId = questionUuid.toString();

        SurveyDatasetResponse.AnswerValue av = new SurveyDatasetResponse.AnswerValue();
        av.setText(a.getAnswerText());
        if (a.getAnswerNumber() != null) {
          double numVal = a.getAnswerNumber().doubleValue();
          av.setNumber(numVal);
          boolean isRootQuestion = question.getParent() == null;
          if (isRootQuestion && !piiMap.containsKey(questionUuid)) {
            numericSum += numVal;
            hasRootNumeric = true;
          }
        }
        if (a.getAnswerDate() != null) {
          av.setDate(a.getAnswerDate().toString());
        }
        av.setProvince(a.getProvince());
        av.setWard(a.getWard());
        av.setAddressDetail(a.getAddressDetail());
        av.setMatrixData(a.getMatrixData());

        List<AnswerOption> answerOptions = answerOptionRepository.findByAnswer_Id(a.getId());
        if (answerOptions != null && !answerOptions.isEmpty()) {
          List<String> optionTexts = new ArrayList<>();
          List<String> optionIds = new ArrayList<>();
          for (AnswerOption ao : answerOptions) {
            if (ao.getOption() != null) {
              if (ao.getOption().getId() != null) {
                optionIds.add(ao.getOption().getId().toString());
              }
              if (ao.getOption().getOptionText() != null) {
                optionTexts.add(ao.getOption().getOptionText());
              }
            }
          }
          av.setOptionIds(optionIds);
          av.setOptionTexts(optionTexts);
        }

        av.setFormatted(formatAnswer(a));
        if (!includePii && piiDetectionService != null && piiMap.containsKey(questionUuid)) {
          piiDetectionService.maskAnswerValue(piiMap.get(questionUuid), av);
        }
        answerMap.put(qId, av);
      }

      Long durationMs = response.getSessionId() != null
              ? sessionDurations.get(response.getSessionId())
              : null;

      records.add(new SurveyDatasetResponse.RecordItem(
              response.getId(),
              response.getRequestId(),
              response.getSubmittedAt(),
              durationMs,
              hasRootNumeric ? numericSum : null,
              answerMap
      ));
    }

    SurveyDatasetResponse result = new SurveyDatasetResponse(
            survey.getId(),
            survey.getTitle(),
            (long) records.size(),
            records
    );
    result.setCanExportData(canExportData);
    result.setPiiIncluded(includePii);
    result.setPiiQuestionIds(new ArrayList<>(piiMap.keySet()));
    Map<UUID, String> piiTypes = new LinkedHashMap<>();
    piiMap.forEach((qid, kind) -> piiTypes.put(qid, kind.name()));
    result.setPiiTypes(piiTypes);
    return result;
  }

  private Map<UUID, Long> loadSessionDurations(UUID surveyId) {
    if (jdbcTemplate == null) {
      return Collections.emptyMap();
    }
    Map<UUID, Long> durations = new HashMap<>();
    jdbcTemplate.query(
        "SELECT id, active_duration_ms FROM survey_sessions WHERE survey_id = ?",
        rs -> {
          Object idObj = rs.getObject("id");
          long duration = rs.getLong("active_duration_ms");
          if (idObj != null && !rs.wasNull()) {
            UUID sessionUuid = idObj instanceof UUID u ? u : UUID.fromString(String.valueOf(idObj));
            durations.put(sessionUuid, duration);
          }
        },
        surveyId
    );
    return durations;
  }

  public byte[] exportResponses(UUID surveyId) {
    guard.exportData(surveyId);

    Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() ->
                    new NotFoundException("Survey không tồn tại"));

    List<Page> pages =
            pageRepository
                    .findBySurvey_IdOrderByOrderIndexAsc(surveyId);

    List<Question> questions = new ArrayList<>();

    for (Page page : pages) {
      questions.addAll(
              questionRepository
                      .findByPage_IdOrderByOrderIndexAsc(
                              page.getId()
                      )
      );
    }

    List<Response> responses =
            responseRepository
                    .findBySurvey_IdOrderBySubmittedAtDesc(
                            surveyId
                    );

    // Xây dựng danh sách các cột xuất (Tách tiêu chí con cho Ma trận)
    List<ExportColumn> exportColumns = new ArrayList<>();

    for (Question question : questions) {
      String pageTitle = question.getPage() != null && question.getPage().getTitle() != null
              ? question.getPage().getTitle()
              : "";
      String prefix = "[" + pageTitle + "] " + question.getQuestionText();
      String type = question.getQuestionType() != null && question.getQuestionType().getCode() != null
              ? question.getQuestionType().getCode().trim().toUpperCase()
              : "";

      boolean isMatrix = "MATRIX_SINGLE".equals(type) || "MATRIX_MULTI".equals(type) || "MATRIX_INPUT".equals(type);

      if (isMatrix && question.getMatrixConfig() != null && !question.getMatrixConfig().isBlank()) {
        try {
          JsonNode configNode = JSON_MAPPER.readTree(question.getMatrixConfig());
          JsonNode rowsNode = configNode.get("rows");
          JsonNode colsNode = configNode.get("columns");

          Map<String, String> colLabels = new LinkedHashMap<>();
          if (colsNode != null && colsNode.isArray()) {
            for (JsonNode col : colsNode) {
              String cId = col.path("id").asText();
              String cLabel = col.path("label").asText(cId);
              colLabels.put(cId, cLabel);
            }
          }

          if (rowsNode != null && rowsNode.isArray() && rowsNode.size() > 0) {
            for (JsonNode r : rowsNode) {
              String rId = r.path("id").asText();
              String rLabel = r.path("label").asText(rId);
              exportColumns.add(new ExportColumn(
                      question,
                      prefix + " - " + rLabel,
                      true,
                      rId,
                      colLabels,
                      type
              ));
            }
            continue;
          }
        } catch (Exception ignored) {}
      }

      exportColumns.add(new ExportColumn(question, prefix, type));
    }

    try (
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream()
    ) {

      Sheet sheet =
              workbook.createSheet("Responses");

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

      CellStyle wrapStyle = workbook.createCellStyle();
      wrapStyle.setWrapText(true);
      wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

      Row header = sheet.createRow(0);

      Cell cell0 = header.createCell(0);
      cell0.setCellValue("Submitted At");
      cell0.setCellStyle(headerStyle);

      int column = 1;

      for (ExportColumn col : exportColumns) {
        Cell cell = header.createCell(column++);
        cell.setCellValue(col.headerText);
        cell.setCellStyle(headerStyle);
      }

      int rowIndex = 1;

      for (Response response : responses) {

        Row row = sheet.createRow(rowIndex++);

        row.createCell(0)
                .setCellValue(
                        response.getSubmittedAt() == null
                                ? ""
                                : response.getSubmittedAt()
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "yyyy-MM-dd HH:mm:ss"
                                        )
                                )
                );

        List<Answer> answers =
                answerRepository
                        .findByResponse_Id(
                                response.getId()
                        );

        Map<UUID, Answer> answerMap =
                new HashMap<>();

        for (Answer answer : answers) {
          answerMap.put(
                  answer.getQuestion().getId(),
                  answer
          );
        }

        column = 1;

        for (ExportColumn col : exportColumns) {
          Answer answer = answerMap.get(col.question.getId());
          Cell cell = row.createCell(column++);

          if (col.isMatrixRow) {
            cell.setCellValue(formatMatrixRowAnswer(answer, col));
          } else if ("DYNAMIC_TABLE".equalsIgnoreCase(col.questionType)) {
            cell.setCellValue(formatDynamicTableAnswer(answer, col.question));
            cell.setCellStyle(wrapStyle);
          } else {
            cell.setCellValue(formatAnswer(answer));
          }
        }
      }

      for (int i = 0; i <= exportColumns.size(); i++) {
        sheet.autoSizeColumn(i);
        if (sheet.getColumnWidth(i) > 60 * 256) {
          sheet.setColumnWidth(i, 60 * 256);
        }
      }

      workbook.write(output);

      return output.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException(
              "Không thể xuất file Excel",
              e
      );
    }
  }

  public byte[] exportAnalysis(UUID surveyId) {
    return exportAnalysis(surveyId, null);
  }

  public byte[] exportAnalysis(
          UUID surveyId,
          ResponseFilterRequest filter
  ) {
    guard.exportData(surveyId);

    Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() ->
                    new NotFoundException("Survey không tồn tại"));

    var statistics =
            statisticsService.getStatistics(
                    surveyId,
                    filter
            );

    try (
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream()
    ) {

      createOverviewSheet(
              workbook,
              survey,
              statistics.getTotalResponses()
      );

      createAnalysisSheet(
              workbook,
              statistics
      );

      workbook.write(output);

      return output.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException(
              "Không thể xuất báo cáo phân tích",
              e
      );
    }
  }

  private void createOverviewSheet(
          Workbook workbook,
          Survey survey,
          Long totalResponses
  ) {

    Sheet sheet =
            workbook.createSheet("Tổng quan");

    Row row0 = sheet.createRow(0);
    row0.createCell(0).setCellValue("Survey ID");
    row0.createCell(1).setCellValue(survey.getId().toString());

    Row row1 = sheet.createRow(1);
    row1.createCell(0).setCellValue("Tên khảo sát");
    row1.createCell(1).setCellValue(
            survey.getTitle() == null
                    ? ""
                    : survey.getTitle()
    );

    Row row2 = sheet.createRow(2);
    row2.createCell(0).setCellValue("Tổng số phiếu");
    row2.createCell(1).setCellValue(totalResponses);

    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
  }

  private void createAnalysisSheet(
          Workbook workbook,
          com.trinhcong1120.survey_service.dto.statistics.SurveyStatisticsResponse statistics
  ) {

    Sheet sheet =
            workbook.createSheet("Phân tích");

    int rowIndex = 0;

    for (var question : statistics.getQuestions()) {

      Row questionRow =
              sheet.createRow(rowIndex++);

      questionRow.createCell(0)
              .setCellValue(
                      question.getQuestionText()
              );

      questionRow.createCell(1)
              .setCellValue(
                      question.getQuestionTypeCode()
              );

      if (question.getChoiceStatistics() != null) {

        Row header = sheet.createRow(rowIndex++);

        header.createCell(0).setCellValue("Option");
        header.createCell(1).setCellValue("Count");
        header.createCell(2).setCellValue("Percent");

        for (var option :
                question.getChoiceStatistics()
                        .getOptions()) {

          Row row =
                  sheet.createRow(rowIndex++);

          row.createCell(0)
                  .setCellValue(
                          option.getOptionText()
                  );

          row.createCell(1)
                  .setCellValue(
                          option.getCount()
                  );

          row.createCell(2)
                  .setCellValue(
                          option.getPercentage()
                  );
        }
      }

      if (question.getNumberStatistics() != null) {

        var number =
                question.getNumberStatistics();

        addMetric(
                sheet,
                rowIndex++,
                "Average",
                number.getAverage()
        );

        addMetric(
                sheet,
                rowIndex++,
                "Min",
                number.getMin()
        );

        addMetric(
                sheet,
                rowIndex++,
                "Max",
                number.getMax()
        );

        addMetric(
                sheet,
                rowIndex++,
                "Median",
                number.getMedian()
        );
      }

      if (question.getDateStatistics() != null) {

        var date =
                question.getDateStatistics();

        addTextMetric(
                sheet,
                rowIndex++,
                "Min Date",
                date.getMin() == null
                        ? ""
                        : date.getMin()
                        .toLocalDate()
                        .toString()
        );

        addTextMetric(
                sheet,
                rowIndex++,
                "Max Date",
                date.getMax() == null
                        ? ""
                        : date.getMax()
                        .toLocalDate()
                        .toString()
        );
      }

      if (question.getAddressStatistics() != null) {

        var address =
                question.getAddressStatistics();

        Row provinceHeader =
                sheet.createRow(rowIndex++);

        provinceHeader.createCell(0)
                .setCellValue("Top tỉnh/thành");

        for (var item :
                address.getTopProvinces()) {

          Row row =
                  sheet.createRow(rowIndex++);

          row.createCell(0)
                  .setCellValue(item.getName());

          row.createCell(1)
                  .setCellValue(item.getCount());
        }

        Row wardHeader =
                sheet.createRow(rowIndex++);

        wardHeader.createCell(0)
                .setCellValue("Top xã/phường");

        for (var item :
                address.getTopWards()) {

          Row row =
                  sheet.createRow(rowIndex++);

          row.createCell(0)
                  .setCellValue(item.getName());

          row.createCell(1)
                  .setCellValue(item.getCount());
        }

        Row addressHeader =
                sheet.createRow(rowIndex++);

        addressHeader.createCell(0)
                .setCellValue("Top địa chỉ");

        for (var item :
                address.getTopAddresses()) {

          Row row =
                  sheet.createRow(rowIndex++);

          row.createCell(0)
                  .setCellValue(
                          item.getProvince()
                                  + " / "
                                  + item.getWard()
                  );

          row.createCell(1)
                  .setCellValue(item.getCount());
        }
      }

      if (question.getTextStatistics() != null) {

        Row header =
                sheet.createRow(rowIndex++);

        header.createCell(0)
                .setCellValue("Nội dung");

        header.createCell(1)
                .setCellValue("Count");

        for (var item :
                question.getTextStatistics()
                        .getTopAnswers()) {

          Row row =
                  sheet.createRow(rowIndex++);

          row.createCell(0)
                  .setCellValue(item.getText());

          row.createCell(1)
                  .setCellValue(item.getCount());
        }
      }

      if (question.getMatrixStatistics() != null) {
        rowIndex = renderMatrixAnalysis(workbook, sheet, rowIndex, question.getMatrixStatistics(), question.getQuestionTypeCode());
      }

      rowIndex++;
    }

    for (int i = 0; i < 15; i++) {
      sheet.autoSizeColumn(i);
      if (sheet.getColumnWidth(i) > 60 * 256) {
        sheet.setColumnWidth(i, 60 * 256);
      }
    }
  }

  private void addMetric(
          Sheet sheet,
          int rowIndex,
          String name,
          Double value
  ) {
    Row row = sheet.createRow(rowIndex);

    row.createCell(0).setCellValue(name);

    if (value != null) {
      row.createCell(1).setCellValue(value);
    }
  }

  private void addTextMetric(
          Sheet sheet,
          int rowIndex,
          String name,
          String value
  ) {
    Row row = sheet.createRow(rowIndex);

    row.createCell(0).setCellValue(name);
    row.createCell(1).setCellValue(value);
  }

  private int renderMatrixAnalysis(
          Workbook workbook,
          Sheet sheet,
          int rowIndex,
          MatrixStatisticsResponse matrixStats,
          String typeCode
  ) {
    if (matrixStats == null) {
      return rowIndex;
    }

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

    // TRƯỜNG HỢP 1: DYNAMIC_TABLE (bảng thống kê theo từng cột)
    if ("DYNAMIC_TABLE".equalsIgnoreCase(typeCode)
            || (matrixStats.getColumns() != null && !matrixStats.getColumns().isEmpty() && (matrixStats.getRows() == null || matrixStats.getRows().isEmpty()))) {

      Row header = sheet.createRow(rowIndex++);
      header.createCell(0).setCellValue("Tên cột");
      header.createCell(1).setCellValue("Loại dữ liệu");
      header.createCell(2).setCellValue("Số lượt điền");
      header.createCell(3).setCellValue("Trung bình (Avg)");
      header.createCell(4).setCellValue("Nhỏ nhất (Min)");
      header.createCell(5).setCellValue("Lớn nhất (Max)");

      for (int c = 0; c <= 5; c++) {
        header.getCell(c).setCellStyle(headerStyle);
      }

      if (matrixStats.getColumns() != null) {
        for (var colStat : matrixStats.getColumns()) {
          Row row = sheet.createRow(rowIndex++);
          row.createCell(0).setCellValue(colStat.getColumnLabel());
          row.createCell(1).setCellValue(colStat.getType() != null ? colStat.getType() : "text");
          row.createCell(2).setCellValue(colStat.getFilledCount() != null ? colStat.getFilledCount() : 0L);

          if (colStat.getAverageValue() != null) {
            row.createCell(3).setCellValue(colStat.getAverageValue());
          } else {
            row.createCell(3).setCellValue("-");
          }
          if (colStat.getMinValue() != null) {
            row.createCell(4).setCellValue(colStat.getMinValue());
          } else {
            row.createCell(4).setCellValue("-");
          }
          if (colStat.getMaxValue() != null) {
            row.createCell(5).setCellValue(colStat.getMaxValue());
          } else {
            row.createCell(5).setCellValue("-");
          }
        }
      }
      return rowIndex + 1; // 1 empty row separator
    }

    // TRƯỜNG HỢP 2: MATRIX_SINGLE, MATRIX_MULTI, MATRIX_INPUT
    List<MatrixStatisticsResponse.RowStatistic> rowStats = matrixStats.getRows();
    if (rowStats == null || rowStats.isEmpty()) {
      return rowIndex;
    }

    List<String> colLabels = new ArrayList<>();
    var firstRowCells = rowStats.get(0).getCells();
    if (firstRowCells != null) {
      for (var cell : firstRowCells) {
        colLabels.add(cell.getColumnLabel());
      }
    }

    boolean hasScore = rowStats.stream().anyMatch(r -> r.getAverageScore() != null);

    // Vẽ Header của Ma trận 2 chiều
    Row header = sheet.createRow(rowIndex++);
    header.createCell(0).setCellValue("Tiêu chí (Hàng)");
    header.getCell(0).setCellStyle(headerStyle);

    int colIdx = 1;
    for (String cLabel : colLabels) {
      Cell c = header.createCell(colIdx++);
      c.setCellValue(cLabel);
      c.setCellStyle(headerStyle);
    }

    Cell totalHeader = header.createCell(colIdx++);
    totalHeader.setCellValue("Tổng số phiếu");
    totalHeader.setCellStyle(headerStyle);

    if (hasScore) {
      Cell scoreHeader = header.createCell(colIdx++);
      scoreHeader.setCellValue("Điểm trung bình");
      scoreHeader.setCellStyle(headerStyle);
    }

    // Điền dữ liệu từng hàng tiêu chí
    for (var rStat : rowStats) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(rStat.getRowLabel());

      int cellColIdx = 1;
      if (rStat.getCells() != null) {
        for (var cellStat : rStat.getCells()) {
          String valStr = cellStat.getCount() + " (" + cellStat.getPercentage() + "%)";
          row.createCell(cellColIdx++).setCellValue(valStr);
        }
      }

      row.createCell(cellColIdx++).setCellValue(rStat.getTotalCount() != null ? rStat.getTotalCount() : 0L);

      if (hasScore) {
        if (rStat.getAverageScore() != null) {
          row.createCell(cellColIdx++).setCellValue(rStat.getAverageScore());
        } else {
          row.createCell(cellColIdx++).setCellValue("-");
        }
      }
    }

    return rowIndex + 1; // 1 empty row separator
  }

  private String formatMatrixRowAnswer(Answer answer, ExportColumn col) {
    if (answer == null || answer.getMatrixData() == null || answer.getMatrixData().isBlank()) {
      return "";
    }
    try {
      JsonNode dataNode = JSON_MAPPER.readTree(answer.getMatrixData());
      if (!dataNode.has(col.rowId)) {
        return "";
      }
      JsonNode rowValueNode = dataNode.get(col.rowId);

      if ("MATRIX_SINGLE".equalsIgnoreCase(col.questionType)) {
        String chosenColId = rowValueNode.asText();
        return col.colLabels.getOrDefault(chosenColId, chosenColId);
      } else if ("MATRIX_MULTI".equalsIgnoreCase(col.questionType)) {
        if (rowValueNode.isArray()) {
          List<String> labels = new ArrayList<>();
          for (JsonNode item : rowValueNode) {
            String cId = item.asText();
            labels.add(col.colLabels.getOrDefault(cId, cId));
          }
          return String.join(", ", labels);
        }
      } else if ("MATRIX_INPUT".equalsIgnoreCase(col.questionType)) {
        if (rowValueNode.isObject()) {
          List<String> parts = new ArrayList<>();
          rowValueNode.fields().forEachRemaining(entry -> {
            String colLabel = col.colLabels.getOrDefault(entry.getKey(), entry.getKey());
            String val = entry.getValue().asText("");
            if (!val.isBlank()) {
              parts.add(colLabel + ": " + val);
            }
          });
          return String.join("; ", parts);
        }
      }
    } catch (Exception ignored) {}
    return "";
  }

  private String formatDynamicTableAnswer(Answer answer, Question question) {
    if (answer == null || answer.getMatrixData() == null || answer.getMatrixData().isBlank()) {
      return "";
    }
    try {
      JsonNode dataNode = JSON_MAPPER.readTree(answer.getMatrixData());
      if (!dataNode.isArray() || dataNode.isEmpty()) {
        return "";
      }

      Map<String, String> colLabels = new LinkedHashMap<>();
      if (question.getMatrixConfig() != null && !question.getMatrixConfig().isBlank()) {
        JsonNode configNode = JSON_MAPPER.readTree(question.getMatrixConfig());
        JsonNode colsNode = configNode.get("columns");
        if (colsNode != null && colsNode.isArray()) {
          for (JsonNode c : colsNode) {
            String cId = c.path("id").asText();
            String cLabel = c.path("label").asText(cId);
            colLabels.put(cId, cLabel);
          }
        }
      }

      List<String> rowsText = new ArrayList<>();
      int rowIndex = 1;
      for (JsonNode rowItem : dataNode) {
        List<String> colTexts = new ArrayList<>();
        if (rowItem.isObject()) {
          rowItem.fields().forEachRemaining(entry -> {
            String cLabel = colLabels.getOrDefault(entry.getKey(), entry.getKey());
            String val = entry.getValue().asText("");
            if (!val.isBlank()) {
              colTexts.add(cLabel + ": " + val);
            }
          });
        }
        if (!colTexts.isEmpty()) {
          rowsText.add("Dòng " + (rowIndex++) + ": [" + String.join(" | ", colTexts) + "]");
        }
      }
      return String.join("\n", rowsText);
    } catch (Exception ignored) {}
    return "";
  }

  private String formatMatrixGeneral(Answer answer, Question question) {
    if (answer == null || answer.getMatrixData() == null || answer.getMatrixData().isBlank()) {
      return "";
    }
    String type = question.getQuestionType() != null && question.getQuestionType().getCode() != null
            ? question.getQuestionType().getCode().trim().toUpperCase()
            : "";
    if ("DYNAMIC_TABLE".equalsIgnoreCase(type)) {
      return formatDynamicTableAnswer(answer, question);
    }
    try {
      JsonNode dataNode = JSON_MAPPER.readTree(answer.getMatrixData());
      Map<String, String> rowLabels = new LinkedHashMap<>();
      Map<String, String> colLabels = new LinkedHashMap<>();

      if (question.getMatrixConfig() != null && !question.getMatrixConfig().isBlank()) {
        JsonNode configNode = JSON_MAPPER.readTree(question.getMatrixConfig());
        JsonNode rowsNode = configNode.get("rows");
        JsonNode colsNode = configNode.get("columns");
        if (rowsNode != null && rowsNode.isArray()) {
          for (JsonNode r : rowsNode) {
            rowLabels.put(r.path("id").asText(), r.path("label").asText());
          }
        }
        if (colsNode != null && colsNode.isArray()) {
          for (JsonNode c : colsNode) {
            colLabels.put(c.path("id").asText(), c.path("label").asText());
          }
        }
      }

      List<String> resultList = new ArrayList<>();
      if (dataNode.isObject()) {
        dataNode.fields().forEachRemaining(entry -> {
          String rLabel = rowLabels.getOrDefault(entry.getKey(), entry.getKey());
          JsonNode val = entry.getValue();
          if (val.isTextual() || val.isNumber()) {
            String colLabel = colLabels.getOrDefault(val.asText(), val.asText());
            resultList.add(rLabel + ": " + colLabel);
          } else if (val.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : val) {
              items.add(colLabels.getOrDefault(item.asText(), item.asText()));
            }
            resultList.add(rLabel + ": [" + String.join(", ", items) + "]");
          } else if (val.isObject()) {
            List<String> items = new ArrayList<>();
            val.fields().forEachRemaining(f -> {
              items.add(colLabels.getOrDefault(f.getKey(), f.getKey()) + ": " + f.getValue().asText());
            });
            resultList.add(rLabel + ": [" + String.join("; ", items) + "]");
          }
        });
      }
      return String.join("; ", resultList);
    } catch (Exception ignored) {}
    return "Đã điền ma trận";
  }

  private String formatAnswer(Answer answer) {

    if (answer == null) {
      return "";
    }

    Question question = answer.getQuestion();

    String type =
            question.getQuestionType() == null
                    ? ""
                    : question.getQuestionType().getCode();

    if ("SINGLE_CHOICE".equalsIgnoreCase(type)
            || "MULTIPLE_CHOICE".equalsIgnoreCase(type)) {

      return answerOptionRepository
              .findByAnswer_Id(answer.getId())
              .stream()
              .map(answerOption ->
                      answerOption.getOption()
                              .getOptionText())
              .filter(Objects::nonNull)
              .reduce(
                      (a, b) -> a + ", " + b
              )
              .orElse("");
    }

    if ("NUMBER".equalsIgnoreCase(type)) {

      return answer.getAnswerNumber() == null
              ? ""
              : String.valueOf(
              answer.getAnswerNumber()
      );
    }

    if ("DATE".equalsIgnoreCase(type)) {

      return answer.getAnswerDate() == null
              ? ""
              : answer.getAnswerDate()
              .format(
                      DateTimeFormatter.ofPattern(
                              "yyyy-MM-dd"
                      )
              );
    }

    if ("ADDRESS".equalsIgnoreCase(type)) {

      String province =
              answer.getProvince() == null
                      ? ""
                      : answer.getProvince();

      String ward =
              answer.getWard() == null
                      ? ""
                      : answer.getWard();

      if (province.isBlank()
              && ward.isBlank()) {
        return "";
      }

      return province + " / " + ward;
    }

    if ("MATRIX_SINGLE".equalsIgnoreCase(type)
            || "MATRIX_MULTI".equalsIgnoreCase(type)
            || "MATRIX_INPUT".equalsIgnoreCase(type)
            || "DYNAMIC_TABLE".equalsIgnoreCase(type)) {
      return formatMatrixGeneral(answer, question);
    }

    return answer.getAnswerText() == null
            ? ""
            : answer.getAnswerText();
  }
}
