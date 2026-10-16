package com.trinhcong1120.survey_service.dto.media;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePresignedUploadRequest {

  @NotBlank(message = "ownerType khong duoc de trong")
  private String ownerType;

  @NotNull(message = "ownerId khong duoc de trong")
  private UUID ownerId;

  @NotBlank(message = "mediaType khong duoc de trong")
  private String mediaType;

  @NotBlank(message = "fileName khong duoc de trong")
  private String fileName;

  @NotBlank(message = "contentType khong duoc de trong")
  private String contentType;

  private Long sizeBytes;

  public CreatePresignedUploadRequest() {}

  public String getOwnerType() { return ownerType; }
  public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

  public UUID getOwnerId() { return ownerId; }
  public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

  public String getMediaType() { return mediaType; }
  public void setMediaType(String mediaType) { this.mediaType = mediaType; }

  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }

  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }

  public Long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}
