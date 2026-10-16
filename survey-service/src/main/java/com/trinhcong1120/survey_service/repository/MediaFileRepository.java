package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

  List<MediaFile> findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(
          String ownerType,
          UUID ownerId
  );

  Optional<MediaFile> findFirstByOwnerTypeAndOwnerIdAndMediaTypeOrderByUploadedAtDesc(
          String ownerType,
          UUID ownerId,
          String mediaType
  );
}
