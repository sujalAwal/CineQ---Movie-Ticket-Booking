package com.awal.cineq.media.repository;

import com.awal.cineq.media.model.Media;
import com.awal.cineq.media.model.MediaType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB Repository for Media
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface MediaRepository extends MongoRepository<Media, String> {

    // Find by file path
    @Query("{ 'filePath': ?0, 'deletedAt': null }")
    List<Media> findByFilePath(String filePath);

    // Find active media by parent ID (for nested folders/files)
    // Returns root-level items when parentId is null
    @Query("{ $or: [ " +
           "  { $and: [ { 'parentId': null }, { $expr: { $eq: [ ?0, null ] } } ] }, " +
           "  { 'parentId': ?0 } " +
           "], 'isActive': true, 'deletedAt': null }")
    List<Media> findActiveMediaByParentId(String parentId);

    // Find all root level active media (parent ID is null or type is FOLDER)
    @Query("{ $and: [ " +
           "  { 'isActive': true }, " +
           "  { 'deletedAt': null }, " +
           "  { $or: [ " +
           "    { 'type': 'FOLDER' }, " +
           "    { 'parentId': null } " +
           "  ] } " +
           "] }")
    List<Media> findAllRootLevelActiveMedia();

    // Return all active folders
    @Query("{ 'type': 'FOLDER', 'isActive': true, 'deletedAt': null }")
    List<Media> findAllActiveFolders();

    // Find all active media (excluding folders) by parent ID with sorting
    @Query(value = "{ $and: [ " +
           "  { $or: [ " +
           "    { $and: [ { 'parentId': null }, { $expr: { $eq: [ ?0, null ] } } ] }, " +
           "    { 'parentId': ?0 } " +
           "  ] }, " +
           "  { 'isActive': true }, " +
           "  { 'deletedAt': null }, " +
           "  { 'type': { $ne: 'FOLDER' } } " +
           "] }", sort = "{ 'createdAt': -1 }")
    List<Media> findActiveMediaByParentIdExcludingFolders(String parentId);
}
