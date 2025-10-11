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

    // ✅ Great methods for finding attachments for one or more posts.
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByPostIdIn(List<Integer> postIds);

    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findByPostId(Integer postId, Pageable pageable);

    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findByPostIdIn(List<Integer> postIds, Pageable pageable);

    // ✅ Standard count method.
    long countByPostId(Integer postId);
    
    // ✅ Efficient batch operation for getting attachment counts for multiple posts.
    @Query("SELECT a.post.id, COUNT(a) FROM Attachment a WHERE a.post.id IN :postIds GROUP BY a.post.id")
    List<Object[]> countAttachmentsByPostIds(@Param("postIds") List<Integer> postIds);

    // ✅ Useful methods for filtering by file type.
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByFileType(String fileType);

    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByFileTypeIn(List<String> fileTypes);
    
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findByPostIdAndFileType(Integer postId, String fileType);

    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT a FROM Attachment a WHERE a.post.id = :postId AND a.fileType IN :fileTypes")
    List<Attachment> findByPostIdAndFileTypeIn(@Param("postId") Integer postId, @Param("fileTypes") List<String> fileTypes);

    // ✅ Great for admin/moderation tasks to monitor storage.
    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT a FROM Attachment a WHERE a.fileSize > :minSize")
    List<Attachment> findLargeAttachments(@Param("minSize") Long minSize);
    
    // ✅ Good for a "recently uploaded" feed.
    @EntityGraph(attributePaths = {"post"})
    List<Attachment> findTop10ByOrderByUploadedAtDesc();

    // ✅ Efficient bulk delete operations.
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.post.id = :postId")
    void deleteByPostId(@Param("postId") Integer postId);
    
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.post.id IN :postIds")
    void deleteByPostIdIn(@Param("postIds") List<Integer> postIds);

    // ✅ Excellent use of EntityGraph to fetch related data efficiently.
    @EntityGraph(attributePaths = {"post", "post.author"})
    Optional<Attachment> findWithPostAndAuthorById(Integer attachmentId);
    
    // ✅ Great analytics queries.
    @Query("SELECT a.fileType, COUNT(a) FROM Attachment a GROUP BY a.fileType")
    List<Object[]> countAttachmentsByFileType();

    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM Attachment a")
    Long getTotalStorageUsed();

    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM Attachment a WHERE a.post.author.userId = :userId")
    Long getStorageUsedByPostAuthor(@Param("userId") Integer userId);

    /**
     * ✅ ADDED: Finds attachments uploaded by a specific user.
     * This is distinct from the author of the post.
     */
    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findByUploadedBy_UserId(Integer userId, Pageable pageable);

    /**
     * ✅ NOTE: In a correctly operating application, these "orphaned attachment" methods
     * should never find or delete anything. The `post` field on the Attachment entity is
     * non-nullable, so the database schema prevents attachments from existing without a post.
     * They are kept here as a failsafe for data cleanup in case of manual database errors.
     */
    @Query("SELECT a FROM Attachment a WHERE a.post IS NULL")
    List<Attachment> findOrphanedAttachments();

    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.post IS NULL")
    void deleteOrphanedAttachments();
    
    // ✅ Standard paginated find method.
    @EntityGraph(attributePaths = {"post"})
    Page<Attachment> findAllByOrderByUploadedAtDesc(Pageable pageable);

    /**
     * ✅ NOTE: `LIKE '%query%'` searches can be slow on very large tables.
     * For high-performance searching, consider a database full-text search index.
     */
    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT a FROM Attachment a WHERE LOWER(a.fileName) LIKE LOWER(CONCAT('%', :filename, '%'))")
    Page<Attachment> findByFileNameContainingIgnoreCase(@Param("filename") String filename, Pageable pageable);
}