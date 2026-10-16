package com.trinhcong1120.survey_service.dto.media;

public class PresignedUploadResponse {

  private String uploadUrl;
  private String method;
  private String bucketName;
  private String objectKey;
  private String objectUrl;
  private Integer expiresInSeconds;

  public PresignedUploadResponse() {}

  public String getUploadUrl() { return uploadUrl; }
  public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }

  public String getMethod() { return method; }
  public void setMethod(String method) { this.method = method; }

  public String getBucketName() { return bucketName; }
  public void setBucketName(String bucketName) { this.bucketName = bucketName; }

  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

  public String getObjectUrl() { return objectUrl; }
  public void setObjectUrl(String objectUrl) { this.objectUrl = objectUrl; }

  public Integer getExpiresInSeconds() { return expiresInSeconds; }
  public void setExpiresInSeconds(Integer expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
  }
}
