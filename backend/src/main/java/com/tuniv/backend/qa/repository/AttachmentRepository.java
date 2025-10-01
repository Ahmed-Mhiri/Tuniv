package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Attachment;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {
    
    // ✅ OPTIMIZED: Single method with EntityGraph for efficient fetching
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByPostIdIn(List<Integer> postIds);

    // ✅ NEW: Efficient bulk deletion
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.post.id = :postId")
    void deleteByPostId(@Param("postId") Integer postId);

    // ✅ NEW: Find by multiple posts with pagination
    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findByPostIdIn(List<Integer> postIds, Pageable pageable);

    // ✅ NEW: Find attachments by post ID with pagination
    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findByPostId(Integer postId, Pageable pageable);

    // ✅ NEW: Count attachments by post ID
    long countByPostId(Integer postId);

    // ✅ NEW: Find attachments by file type
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByFileType(String fileType);

    // ✅ NEW: Find attachments by multiple file types
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByFileTypeIn(List<String> fileTypes);

    // ✅ NEW: Find large attachments (above certain size)
    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT a FROM Attachment a WHERE a.fileSize > :minSize")
    List<Attachment> findLargeAttachments(@Param("minSize") Long minSize);

    // ✅ NEW: Find recent attachments
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findTop10ByOrderByUploadedAtDesc();

    // ✅ NEW: Find attachments by post and file type
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByPostIdAndFileType(Integer postId, String fileType);

    // ✅ NEW: Batch delete attachments by post IDs
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.post.id IN :postIds")
    void deleteByPostIdIn(@Param("postIds") List<Integer> postIds);

    // ✅ NEW: Find attachment with post and author details
    @EntityGraph(attributePaths = {"post", "post.author"})
    Optional<Attachment> findWithPostAndAuthorById(Integer attachmentId);

    // ✅ NEW: Count attachments by file type
    @Query("SELECT a.fileType, COUNT(a) FROM Attachment a GROUP BY a.fileType")
    List<Object[]> countAttachmentsByFileType();

    // ✅ NEW: Find orphaned attachments (attachments without posts)
    @Query("SELECT a FROM Attachment a WHERE a.post IS NULL")
    List<Attachment> findOrphanedAttachments();

    // ✅ NEW: Efficient cleanup of orphaned attachments
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.post IS NULL")
    void deleteOrphanedAttachments();

    // ✅ NEW: Get total storage used by attachments
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM Attachment a")
    Long getTotalStorageUsed();

    // ✅ NEW: Get storage used by specific user's attachments
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM Attachment a WHERE a.post.author.userId = :userId")
    Long getStorageUsedByUser(@Param("userId") Integer userId);

    // ✅ NEW: Find attachments with pagination and sorting
    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findAllByOrderByUploadedAtDesc(Pageable pageable);

    // ✅ NEW: Search attachments by filename
    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT a FROM Attachment a WHERE LOWER(a.fileName) LIKE LOWER(CONCAT('%', :filename, '%'))")
    Page<Attachment> findByFileNameContainingIgnoreCase(@Param("filename") String filename, Pageable pageable);

    // ✅ NEW: Find attachments by post ID with specific file types
    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT a FROM Attachment a WHERE a.post.id = :postId AND a.fileType IN :fileTypes")
    List<Attachment> findByPostIdAndFileTypeIn(@Param("postId") Integer postId, @Param("fileTypes") List<String> fileTypes);

    // ✅ NEW: Get attachment count by post IDs (batch operation)
    @Query("SELECT a.post.id, COUNT(a) FROM Attachment a WHERE a.post.id IN :postIds GROUP BY a.post.id")
    List<Object[]> countAttachmentsByPostIds(@Param("postIds") List<Integer> postIds);
}