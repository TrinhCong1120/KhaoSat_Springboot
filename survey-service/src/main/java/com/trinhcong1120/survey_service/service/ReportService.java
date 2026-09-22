package com.trinhcong1120.survey_service.service;

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
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final StatisticsService statisticsService;

  public ReportService(
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          StatisticsService statisticsService
  ) {
    this.surveyRepository = surveyRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.statisticsService = statisticsService;
  }

  public byte[] exportResponses(Integer surveyId) {

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

    try (
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream()
    ) {

      Sheet sheet =
              workbook.createSheet("Responses");

      Row header = sheet.createRow(0);

      header.createCell(0)
              .setCellValue("Submitted At");

      int column = 1;

      for (Question question : questions) {

        String pageTitle =
                question.getPage().getTitle();

        String headerText =
                "[" + pageTitle + "] "
                        + question.getQuestionText();

        header.createCell(column++)
                .setCellValue(headerText);
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

        Map<Integer, Answer> answerMap =
                new HashMap<>();

        for (Answer answer : answers) {
          answerMap.put(
                  answer.getQuestion().getId(),
                  answer
          );
        }

        column = 1;

        for (Question question : questions) {

          Answer answer =
                  answerMap.get(question.getId());

          row.createCell(column++)
                  .setCellValue(
                          formatAnswer(answer)
                  );
        }
      }

      for (int i = 0;
           i <= questions.size();
           i++) {

        sheet.autoSizeColumn(i);
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

  public byte[] exportAnalysis(Integer surveyId) {
    return exportAnalysis(surveyId, null);
  }

  public byte[] exportAnalysis(
          Integer surveyId,
          ResponseFilterRequest filter
  ) {

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
    row0.createCell(1).setCellValue(survey.getId());

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

      rowIndex++;
    }

    for (int i = 0; i < 5; i++) {
      sheet.autoSizeColumn(i);
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

    return answer.getAnswerText() == null
            ? ""
            : answer.getAnswerText();
  }
}
