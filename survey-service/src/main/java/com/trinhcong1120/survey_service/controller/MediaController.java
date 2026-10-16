package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.CompleteMediaUploadRequest;
import com.trinhcong1120.survey_service.dto.media.CreatePresignedUploadRequest;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.media.PresignedUploadResponse;
import com.trinhcong1120.survey_service.service.MediaStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Media")
public class MediaController {

  private final MediaStorageService mediaStorageService;

  public MediaController(MediaStorageService mediaStorageService) {
    this.mediaStorageService = mediaStorageService;
  }

  @PostMapping("/presigned-url")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<PresignedUploadResponse> createPresignedUpload(
          @Valid @RequestBody CreatePresignedUploadRequest request,
          HttpServletRequest httpRequest
  ) {
    String publicOrigin = ServletUriComponentsBuilder.fromRequestUri(httpRequest)
            .replacePath(null)
            .replaceQuery(null)
            .build()
            .toUriString();
    return ResponseEntity.ok(
            mediaStorageService.createPresignedUpload(request, publicOrigin)
    );
  }

  @PostMapping("/complete")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<MediaUploadResponse> completeUpload(
          @Valid @RequestBody CompleteMediaUploadRequest request
  ) {
    return ResponseEntity.ok(
            mediaStorageService.completeUpload(request)
    );
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<MediaUploadResponse> addMedia(
          @Valid @RequestBody CompleteMediaUploadRequest request
  ) {
    return ResponseEntity.ok(
            mediaStorageService.completeUpload(request)
    );
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all', 'survey_update_all')")
  public ResponseEntity<List<MediaUploadResponse>> getByOwner(
          @RequestParam String ownerType,
          @RequestParam UUID ownerId
  ) {
    return ResponseEntity.ok(
            mediaStorageService.getByOwner(
                    ownerType,
                    ownerId
            )
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<Map<String, Object>> deleteMedia(
          @PathVariable UUID id
  ) {
    mediaStorageService.deleteMedia(id);

    return ResponseEntity.ok(
            Map.of(
                    "message", "Da xoa media",
                    "mediaId", id
            )
    );
  }
}
