package com.trinhcong1120.survey_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.failed.FailedSurveyResponse;
import com.trinhcong1120.survey_service.dto.failed.ImportFailedSurveyResponse;
import com.trinhcong1120.survey_service.dto.submit.SubmitAnswerRequest;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service
public class FailedSurveyService {

  private static final Path FAILED_DIRECTORY =
          Paths.get(
                  "public",
                  "failed-surveys"
          ).toAbsolutePath().normalize();

  private final ObjectMapper objectMapper;

  private final SurveyRepository surveyRepository;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;

  public FailedSurveyService(
          ObjectMapper objectMapper,
          SurveyRepository surveyRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository
  ) {
    this.objectMapper = objectMapper;
    this.surveyRepository = surveyRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
  }

  public List<FailedSurveyResponse> getFiles(
          boolean includeContent
  ) {

    if (!Files.exists(FAILED_DIRECTORY)) {
      return new ArrayList<>();
    }

    try (var stream =
                 Files.list(FAILED_DIRECTORY)) {

      return stream
              .filter(Files::isRegularFile)
              .filter(path ->
                      path.getFileName()
                              .toString()
                              .toLowerCase()
                              .endsWith(".json"))
              .sorted()
              .map(path ->
                      toResponse(
                              path,
                              includeContent
                      ))
              .toList();

    } catch (IOException e) {
      throw new RuntimeException(
              "Không thể đọc thư mục failed-surveys",
              e
      );
    }
  }

  public byte[] download(String fileName) {

    Path path = resolveSafeFile(fileName);

    if (!Files.exists(path)
            || !Files.isRegularFile(path)) {

      throw new NotFoundException(
              "File không tồn tại"
      );
    }

    try {
      return Files.readAllBytes(path);

    } catch (IOException e) {
      throw new RuntimeException(
              "Không thể đọc file",
              e
      );
    }
  }

  public String deleteOne(String fileName) {

    Path path = resolveSafeFile(fileName);

    if (!Files.exists(path)
            || !Files.isRegularFile(path)) {

      throw new NotFoundException(
              "File không tồn tại"
      );
    }

    try {

      Files.delete(path);

      return path.getFileName().toString();

    } catch (IOException e) {
      throw new RuntimeException(
              "Không thể xóa file",
              e
      );
    }
  }

  public List<String> deleteAll() {

    if (!Files.exists(FAILED_DIRECTORY)) {
      return new ArrayList<>();
    }

    List<String> deleted =
            new ArrayList<>();

    try (var stream =
                 Files.list(FAILED_DIRECTORY)) {

      List<Path> files =
              stream
                      .filter(Files::isRegularFile)
                      .toList();

      for (Path path : files) {

        Files.delete(path);

        deleted.add(
                path.getFileName()
                        .toString()
        );
      }

      return deleted;

    } catch (IOException e) {
      throw new RuntimeException(
              "Không thể xóa file",
              e
      );
    }
  }

  public ImportFailedSurveyResponse importFile(
          MultipartFile file
  ) {

    if (file == null || file.isEmpty()) {
      throw new BadRequestException(
              "File không được để trống"
      );
    }

    String fileName =
            file.getOriginalFilename();

    if (fileName == null
            || !fileName.toLowerCase()
            .endsWith(".json")) {

      throw new BadRequestException(
              "File phải có định dạng .json"
      );
    }

    List<JsonNode> records;

    try {

      records =
              objectMapper.readValue(
                      file.getBytes(),
                      new TypeReference<List<JsonNode>>() {
                      }
              );

    } catch (Exception e) {

      throw new BadRequestException(
              "JSON không hợp lệ"
      );
    }

    int total = records.size();
    int imported = 0;
    int skippedDuplicate = 0;
    int failedRemain = 0;

    for (JsonNode record : records) {

      try {

        JsonNode data =
                getIgnoreCase(
                        record,
                        "data"
                );

        if (data == null
                || data.isNull()) {

          failedRemain++;
          continue;
        }

        UUID requestId =
                parseRequestId(record);

        if (requestId == null) {
          failedRemain++;
          continue;
        }

        if (responseRepository
                .existsByRequestId(
                        requestId
                )) {

          skippedDuplicate++;
          continue;
        }

        Integer surveyId =
                getIntegerIgnoreCase(
                        record,
                        "surveyId"
                );

        if (surveyId == null) {

          surveyId =
                  getIntegerIgnoreCase(
                          data,
                          "surveyId"
                  );
        }

        if (surveyId == null) {
          failedRemain++;
          continue;
        }

        Survey survey =
                surveyRepository
                        .findById(surveyId)
                        .orElse(null);

        if (survey == null) {
          failedRemain++;
          continue;
        }

        JsonNode answersNode =
                getIgnoreCase(
                        data,
                        "answers"
                );

        if (answersNode == null
                || !answersNode.isArray()) {

          failedRemain++;
          continue;
        }

        List<SubmitAnswerRequest> answers =
                objectMapper.convertValue(
                        answersNode,
                        new TypeReference<
                                List<SubmitAnswerRequest>>() {
                        }
                );

        if (!validateAnswers(answers)) {
          failedRemain++;
          continue;
        }

        saveRecord(
                survey,
                requestId,
                record,
                answers
        );

        imported++;

      } catch (Exception e) {

        failedRemain++;
      }
    }

    return new ImportFailedSurveyResponse(
            total,
            imported,
            skippedDuplicate,
            failedRemain
    );
  }

  private boolean validateAnswers(
          List<SubmitAnswerRequest> answers
  ) {

    if (answers == null) {
      return false;
    }

    for (SubmitAnswerRequest item : answers) {

      if (item.getQuestionId() == null) {
        return false;
      }

      Question question =
              questionRepository
                      .findById(
                              item.getQuestionId()
                      )
                      .orElse(null);

      if (question == null) {
        return false;
      }

      if (item.getOptionIds() == null) {
        continue;
      }

      for (Integer optionId :
              item.getOptionIds()) {

        if (optionId == null) {
          return false;
        }

        Option option =
                optionRepository
                        .findById(optionId)
                        .orElse(null);

        if (option == null) {
          return false;
        }

        if (!Objects.equals(
                option.getQuestion().getId(),
                question.getId()
        )) {
          return false;
        }
      }
    }

    return true;
  }

  private void saveRecord(
          Survey survey,
          UUID requestId,
          JsonNode record,
          List<SubmitAnswerRequest> answers
  ) {

    Response response =
            new Response();

    response.setSurvey(survey);
    response.setRequestId(requestId);

    LocalDateTime submittedAt =
            parseDateTime(
                    getIgnoreCase(
                            record,
                            "failedAt"
                    )
            );

    if (submittedAt == null) {
      submittedAt = LocalDateTime.now();
    }

    response.setSubmittedAt(submittedAt);

    response =
            responseRepository.save(response);

    for (SubmitAnswerRequest item : answers) {

      Question question =
              questionRepository
                      .findById(
                              item.getQuestionId()
                      )
                      .orElseThrow();

      Answer answer =
              new Answer();

      answer.setResponse(response);
      answer.setQuestion(question);

      answer.setAnswerText(
              item.getAnswerText()
      );

      answer.setAnswerNumber(
              item.getAnswerNumber()
      );

      answer.setAnswerDate(
              item.getAnswerDate()
      );

      answer.setProvinceCode(
              item.getProvinceCode()
      );

      answer.setWardCode(
              item.getWardCode()
      );

      answer.setProvince(
              item.getProvince()
      );

      answer.setWard(
              item.getWard()
      );

      answer =
              answerRepository.save(answer);

      if (item.getOptionIds() == null) {
        continue;
      }

      for (Integer optionId :
              item.getOptionIds()) {

        Option option =
                optionRepository
                        .findById(optionId)
                        .orElseThrow();

        AnswerOption answerOption =
                new AnswerOption();

        answerOption.setAnswer(answer);
        answerOption.setOption(option);

        answerOptionRepository.save(
                answerOption
        );
      }
    }
  }

  private FailedSurveyResponse toResponse(
          Path path,
          boolean includeContent
  ) {

    try {

      String content =
              includeContent
                      ? Files.readString(path)
                      : null;

      LocalDateTime lastModified =
              LocalDateTime.ofInstant(
                      Files.getLastModifiedTime(path)
                              .toInstant(),
                      ZoneId.systemDefault()
              );

      return new FailedSurveyResponse(
              path.getFileName().toString(),
              Files.size(path),
              lastModified,
              content
      );

    } catch (IOException e) {
      throw new RuntimeException(
              "Không thể đọc file",
              e
      );
    }
  }

  private Path resolveSafeFile(
          String fileName
  ) {

    if (fileName == null
            || fileName.isBlank()) {

      throw new BadRequestException(
              "Tên file không hợp lệ"
      );
    }

    String safeName =
            Paths.get(fileName)
                    .getFileName()
                    .toString();

    if (!safeName.equals(fileName)) {
      throw new BadRequestException(
              "Tên file không hợp lệ"
      );
    }

    Path path =
            FAILED_DIRECTORY
                    .resolve(safeName)
                    .normalize();

    if (!path.startsWith(
            FAILED_DIRECTORY)) {

      throw new BadRequestException(
              "Đường dẫn không hợp lệ"
      );
    }

    return path;
  }

  private UUID parseRequestId(
          JsonNode record
  ) {

    JsonNode node =
            getIgnoreCase(
                    record,
                    "requestId"
            );

    if (node == null
            || node.isNull()
            || node.asText().isBlank()) {

      return null;
    }

    try {
      return UUID.fromString(
              node.asText()
      );
    } catch (Exception e) {
      return null;
    }
  }

  private LocalDateTime parseDateTime(
          JsonNode node
  ) {

    if (node == null
            || node.isNull()
            || node.asText().isBlank()) {

      return null;
    }

    try {
      return LocalDateTime.parse(
              node.asText()
      );
    } catch (Exception e) {
      return null;
    }
  }

  private JsonNode getIgnoreCase(
          JsonNode node,
          String name
  ) {

    if (node == null
            || !node.isObject()) {
      return null;
    }

    Iterator<Map.Entry<String, JsonNode>>
            fields = node.fields();

    while (fields.hasNext()) {

      Map.Entry<String, JsonNode> entry =
              fields.next();

      if (entry.getKey()
              .equalsIgnoreCase(name)) {

        return entry.getValue();
      }
    }

    return null;
  }

  private Integer getIntegerIgnoreCase(
          JsonNode node,
          String name
  ) {

    JsonNode value =
            getIgnoreCase(node, name);

    if (value == null
            || value.isNull()) {
      return null;
    }

    try {
      return value.asInt();
    } catch (Exception e) {
      return null;
    }
  }
}