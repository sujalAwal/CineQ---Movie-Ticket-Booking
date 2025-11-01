package com.awal.cineq.media.repository;

import com.awal.cineq.media.model.Media;
import com.awal.cineq.media.model.MediaType;
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
    // Updated to return root-level items when parentId is null (m.parentId IS NULL)
    @Query("SELECT m FROM Media m WHERE ((:parentId IS NULL AND m.parentId IS NULL) OR m.parentId = :parentId) " +
            "AND m.isActive = true AND m.deletedAt IS NULL")
    List<Media> findActiveMediaByParentId(@Param("parentId") UUID parentId);

    // Find all root level active media (parent ID is null)
    @Query("SELECT m FROM Media m WHERE m.isActive = true AND m.deletedAt IS NULL " +
            "AND (m.type = com.awal.cineq.media.model.MediaType.FOLDER OR m.parentId IS NULL)")
    List<Media> findAllRootLevelActiveMedia();

    //Return all folder
    @Query("SELECT m FROM Media m WHERE m.type = MediaType.FOLDER AND m.isActive = true AND m.deletedAt IS NULL")
    List<Media> findAllActiveFolders();

    // Find all active media (excluding folders) by parent ID with sorting
    @Query("SELECT m FROM Media m WHERE " +
           "(:parentId IS NULL AND m.parentId IS NULL OR m.parentId = :parentId) " +
           "AND m.isActive = true " +
           "AND m.deletedAt IS NULL " +
           "AND m.type != com.awal.cineq.media.model.MediaType.FOLDER " +
           "ORDER BY m.createdAt DESC")
    List<Media> findActiveMediaByParentIdExcludingFolders(@Param("parentId") UUID parentId);
}
