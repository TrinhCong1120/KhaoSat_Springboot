package com.trinhcong1120.survey_service.dto.media;

import com.trinhcong1120.survey_service.entity.MediaFile;
import java.util.UUID;

import java.time.LocalDateTime;

public class MediaUploadResponse {

  private UUID id;
  private String ownerType;
  private UUID ownerId;
  private String mediaType;
  private String originalFilename;
  private String contentType;
  private Long sizeBytes;
  private String bucketName;
  private String objectKey;
  private String objectUrl;
  private UUID createdByUserId;
  private LocalDateTime uploadedAt;

  public MediaUploadResponse() {}

  public static MediaUploadResponse fromEntity(MediaFile mediaFile) {
    MediaUploadResponse response = new MediaUploadResponse();
    response.setId(mediaFile.getId());
    response.setOwnerType(mediaFile.getOwnerType());
    response.setOwnerId(mediaFile.getOwnerId());
    response.setMediaType(mediaFile.getMediaType());
    response.setOriginalFilename(mediaFile.getOriginalFilename());
    response.setContentType(mediaFile.getContentType());
    response.setSizeBytes(mediaFile.getSizeBytes());
    response.setBucketName(mediaFile.getBucketName());
    response.setObjectKey(mediaFile.getObjectKey());
    response.setObjectUrl(mediaFile.getObjectUrl());
    response.setCreatedByUserId(mediaFile.getCreatedByUserId());
    response.setUploadedAt(mediaFile.getUploadedAt());
    return response;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getOwnerType() { return ownerType; }
  public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

  public UUID getOwnerId() { return ownerId; }
  public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

  public String getMediaType() { return mediaType; }
  public void setMediaType(String mediaType) { this.mediaType = mediaType; }

  public String getOriginalFilename() { return originalFilename; }
  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }

  public Long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

  public String getBucketName() { return bucketName; }
  public void setBucketName(String bucketName) { this.bucketName = bucketName; }

  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

  public String getObjectUrl() { return objectUrl; }
  public void setObjectUrl(String objectUrl) { this.objectUrl = objectUrl; }

  public UUID getCreatedByUserId() { return createdByUserId; }
  public void setCreatedByUserId(UUID createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public LocalDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
