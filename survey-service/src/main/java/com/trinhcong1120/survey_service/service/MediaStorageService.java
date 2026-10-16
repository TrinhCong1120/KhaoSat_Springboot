package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.media.CompleteMediaUploadRequest;
import com.trinhcong1120.survey_service.dto.media.CreatePresignedUploadRequest;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.media.PresignedUploadResponse;
import com.trinhcong1120.survey_service.entity.MediaFile;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.MediaFileRepository;
import com.trinhcong1120.survey_service.repository.OptionRepository;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.QuestionRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class MediaStorageService {

  private final MinioClient minioClient;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final MediaFileRepository mediaFileRepository;
  private final SurveyRepository surveyRepository;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final String bucketName;
  private final String accessKey;
  private final String secretKey;
  private final String region;
  private final Integer presignedExpirySeconds;

  public MediaStorageService(
          MinioClient minioClient,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          MediaFileRepository mediaFileRepository,
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          @Value("${app.minio.bucket}") String bucketName,
          @Value("${app.minio.access-key}") String accessKey,
          @Value("${app.minio.secret-key}") String secretKey,
          @Value("${app.minio.region:us-east-1}") String region,
          @Value("${app.minio.presigned-expiry-seconds:600}") Integer presignedExpirySeconds
  ) {
    this.minioClient = minioClient;
    this.guard = guard;
    this.mediaFileRepository = mediaFileRepository;
    this.surveyRepository = surveyRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.bucketName = bucketName;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.presignedExpirySeconds = presignedExpirySeconds;
  }

  public PresignedUploadResponse createPresignedUpload(
          CreatePresignedUploadRequest request,
          String publicOrigin
  ) {
    String normalizedOwnerType = normalizeOwnerType(request.getOwnerType());
    guard.editMediaOwner(normalizedOwnerType, request.getOwnerId());
    String normalizedMediaType = normalizeMediaType(request.getMediaType());

    validateContentType(request.getContentType(), normalizedMediaType);
    assertOwnerExists(normalizedOwnerType, request.getOwnerId());

    try {
      ensureBucket();

      String objectKey =
              buildObjectKey(
                      normalizedOwnerType,
                      request.getOwnerId(),
                      normalizedMediaType,
                      request.getFileName()
              );

      MinioClient publicMinioClient = MinioClient.builder()
              .endpoint(publicOrigin)
              .credentials(accessKey, secretKey)
              .region(region)
              .build();
      String uploadUrl =
              publicMinioClient.getPresignedObjectUrl(
                      GetPresignedObjectUrlArgs.builder()
                              .method(Method.PUT)
                              .bucket(bucketName)
                              .object(objectKey)
                              .expiry(presignedExpirySeconds)
                              .build()
              );

      PresignedUploadResponse response = new PresignedUploadResponse();
      response.setUploadUrl(publicOrigin + "/api" + uploadUrl.substring(publicOrigin.length()));
      response.setMethod("PUT");
      response.setBucketName(bucketName);
      response.setObjectKey(objectKey);
      response.setObjectUrl(buildObjectUrl(objectKey));
      response.setExpiresInSeconds(presignedExpirySeconds);

      return response;

    } catch (BadRequestException | NotFoundException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("Khong tao duoc presigned URL");
    }
  }

  public MediaUploadResponse completeUpload(
          CompleteMediaUploadRequest request
  ) {
    String normalizedOwnerType = normalizeOwnerType(request.getOwnerType());
    guard.editMediaOwner(normalizedOwnerType, request.getOwnerId());
    String normalizedMediaType = normalizeMediaType(request.getMediaType());

    assertOwnerExists(normalizedOwnerType, request.getOwnerId());
    validateObjectKey(
            request.getObjectKey(),
            normalizedOwnerType,
            request.getOwnerId(),
            normalizedMediaType
    );

    StatObjectResponse statObject = statObject(request.getObjectKey());
    String contentType =
            request.getContentType() == null || request.getContentType().isBlank()
                    ? statObject.contentType()
                    : request.getContentType();

    validateContentType(contentType, normalizedMediaType);

    String objectUrl = buildObjectUrl(request.getObjectKey());

    MediaFile mediaFile = new MediaFile();
    mediaFile.setOwnerType(normalizedOwnerType);
    mediaFile.setOwnerId(request.getOwnerId());
    mediaFile.setMediaType(normalizedMediaType);
    mediaFile.setOriginalFilename(request.getOriginalFilename());
    mediaFile.setContentType(contentType);
    mediaFile.setSizeBytes(
            request.getSizeBytes() == null
                    ? statObject.size()
                    : request.getSizeBytes()
    );
    mediaFile.setBucketName(bucketName);
    mediaFile.setObjectKey(request.getObjectKey());
    mediaFile.setObjectUrl(objectUrl);
    mediaFile.setCreatedByUserId(guard.userId());
    mediaFile.setUploadedAt(LocalDateTime.now());

    mediaFile = mediaFileRepository.save(mediaFile);

    return MediaUploadResponse.fromEntity(mediaFile);
  }

  public void deleteMedia(UUID id) {
    if (id == null) {
      throw new BadRequestException("mediaId khong duoc de trong");
    }

    MediaFile mediaFile =
            mediaFileRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Media khong ton tai"));

    guard.editMedia(id);

    removeObjectIfExists(mediaFile.getObjectKey());
    mediaFileRepository.delete(mediaFile);
    mediaFileRepository.flush();
  }

  @Transactional(readOnly = true)
  public List<MediaUploadResponse> getByOwner(
          String ownerType,
          UUID ownerId
  ) {
    String normalizedOwnerType = normalizeOwnerType(ownerType);
    guard.viewMediaOwner(normalizedOwnerType, ownerId);

    if (ownerId == null) {
      throw new BadRequestException("ownerId khong duoc de trong");
    }

    return mediaFileRepository
            .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(
                    normalizedOwnerType,
                    ownerId
            )
            .stream()
            .map(MediaUploadResponse::fromEntity)
            .toList();
  }

  private void ensureBucket() throws Exception {
    boolean exists =
            minioClient.bucketExists(
                    BucketExistsArgs.builder()
                      .bucket(bucketName)
                      .build()
            );

    if (!exists) {
      minioClient.makeBucket(
              MakeBucketArgs.builder()
                      .bucket(bucketName)
                      .build()
      );
    }

    minioClient.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(publicReadPolicy())
                    .build()
    );
  }

  private StatObjectResponse statObject(String objectKey) {
    try {
      return minioClient.statObject(
              StatObjectArgs.builder()
                      .bucket(bucketName)
                      .object(objectKey)
                      .build()
      );
    } catch (Exception ex) {
      throw new BadRequestException("File chua ton tai tren MinIO");
    }
  }

  private void removeObjectIfExists(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return;
    }

    try {
      minioClient.removeObject(
              RemoveObjectArgs.builder()
                      .bucket(bucketName)
                      .object(objectKey)
                      .build()
      );
    } catch (Exception ignored) {
    }
  }

  private String publicReadPolicy() {
    return """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {"AWS": ["*"]},
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """.formatted(bucketName);
  }

  private void validateContentType(
          String contentType,
          String mediaType
  ) {
    if (contentType == null || contentType.isBlank()) {
      throw new BadRequestException("contentType khong duoc de trong");
    }

    String value = contentType.toLowerCase(Locale.ROOT);

    boolean valid = switch (mediaType) {
      case "IMAGE" -> value.startsWith("image/");
      case "VIDEO" -> value.startsWith("video/");
      case "AUDIO" -> value.startsWith("audio/");
      default -> false;
    };

    if (!valid) {
      throw new BadRequestException("Loai file khong khop mediaType");
    }
  }

  private void validateObjectKey(
          String objectKey,
          String ownerType,
          UUID ownerId,
          String mediaType
  ) {
    if (objectKey == null || objectKey.isBlank()) {
      throw new BadRequestException("objectKey khong duoc de trong");
    }

    String expectedPrefix =
            ownerType.toLowerCase(Locale.ROOT)
                    + "/"
                    + ownerId
                    + "/"
                    + mediaType.toLowerCase(Locale.ROOT)
                    + "/";

    if (!objectKey.startsWith(expectedPrefix)) {
      throw new BadRequestException("objectKey khong khop owner/media");
    }
  }

  private void assertOwnerExists(
          String ownerType,
          UUID ownerId
  ) {
    if (ownerId == null) {
      throw new BadRequestException("ownerId khong duoc de trong");
    }

    switch (ownerType) {
      case "SURVEY" -> {
        if (!surveyRepository.existsById(ownerId)) {
          throw new NotFoundException("Survey khong ton tai");
        }
      }
      case "PAGE" -> {
        if (!pageRepository.existsById(ownerId)) {
          throw new NotFoundException("Page khong ton tai");
        }
      }
      case "QUESTION" -> {
        if (!questionRepository.existsById(ownerId)) {
          throw new NotFoundException("Question khong ton tai");
        }
      }
      case "OPTION" -> {
        if (!optionRepository.existsById(ownerId)) {
          throw new NotFoundException("Option khong ton tai");
        }
      }
      default -> throw new BadRequestException("ownerType khong hop le");
    }
  }

  private String normalizeOwnerType(String ownerType) {
    if (ownerType == null || ownerType.isBlank()) {
      throw new BadRequestException("ownerType khong duoc de trong");
    }

    String value = ownerType.trim().toUpperCase(Locale.ROOT);
    if (!value.equals("SURVEY")
            && !value.equals("PAGE")
            && !value.equals("QUESTION")
            && !value.equals("OPTION")) {
      throw new BadRequestException("ownerType khong hop le");
    }

    return value;
  }

  private String normalizeMediaType(String mediaType) {
    if (mediaType == null || mediaType.isBlank()) {
      throw new BadRequestException("mediaType khong duoc de trong");
    }

    String value = mediaType.trim().toUpperCase(Locale.ROOT);
    if (!value.equals("IMAGE")
            && !value.equals("VIDEO")
            && !value.equals("AUDIO")) {
      throw new BadRequestException("mediaType khong hop le");
    }

    return value;
  }

  private String buildObjectKey(
          String ownerType,
          UUID ownerId,
          String mediaType,
          String originalFilename
  ) {
    String filename =
            originalFilename == null || originalFilename.isBlank()
                    ? "file"
                    : originalFilename;

    filename = filename
            .replace("\\", "_")
            .replace("/", "_")
            .replaceAll("[^a-zA-Z0-9._-]", "_");

    return ownerType.toLowerCase(Locale.ROOT)
            + "/"
            + ownerId
            + "/"
            + mediaType.toLowerCase(Locale.ROOT)
            + "/"
            + UUID.randomUUID()
            + "_"
            + filename;
  }

  private String buildObjectUrl(String objectKey) {
    return "/api/"
            + bucketName
            + "/"
            + objectKey;
  }


}
