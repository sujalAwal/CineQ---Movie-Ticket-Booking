package com.awal.cineq.media.repository;

import com.awal.cineq.media.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    // Find by file path
    List<Media> findByFilePath(String filePath);

    // Find active media by parent ID (for nested folders/files)
    @Query("SELECT m FROM Media m WHERE m.parentId = :parentId AND m.isActive = true AND m.deletedAt IS NULL")
    List<Media> findActiveMediaByParentId(@Param("parentId") UUID parentId);

    // Find all root level active media (parent ID is null)
    @Query("SELECT m FROM Media m WHERE m.parentId IS NULL AND m.isActive = true AND m.deletedAt IS NULL")
    List<Media> findAllRootLevelActiveMedia();
}

