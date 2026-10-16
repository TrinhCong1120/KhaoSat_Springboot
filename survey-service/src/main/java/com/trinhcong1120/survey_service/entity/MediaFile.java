package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
public class MediaFile {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_type", length = 50, nullable = false)
  private String ownerType;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "media_type", length = 50, nullable = false)
  private String mediaType;

  @Column(name = "original_filename", length = 500)
  private String originalFilename;

  @Column(name = "content_type", length = 255)
  private String contentType;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "bucket_name", length = 255, nullable = false)
  private String bucketName;

  @Column(name = "object_key", length = 1000, nullable = false)
  private String objectKey;

  @Column(name = "object_url", columnDefinition = "TEXT", nullable = false)
  private String objectUrl;

  @Column(name = "created_by_user_id")
  private UUID createdByUserId;

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;

  public MediaFile() {}

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
