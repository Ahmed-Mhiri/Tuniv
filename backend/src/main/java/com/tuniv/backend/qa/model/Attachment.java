package com.tuniv.backend.qa.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "attachments", indexes = {
    @Index(name = "idx_attachment_post", columnList = "post_id"),
    @Index(name = "idx_attachment_type", columnList = "file_type"),
    @Index(name = "idx_attachment_uploaded", columnList = "uploaded_at DESC"),
    @Index(name = "idx_attachment_user", columnList = "uploaded_by_user_id"),
    @Index(name = "idx_attachment_mimetype", columnList = "mime_type"),
    @Index(name = "idx_attachment_public", columnList = "is_public, uploaded_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class Attachment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Integer attachmentId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== FILE INFORMATION ==========
    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank
    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @NotBlank
    @Size(max = 50)
    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotBlank
    @Size(max = 100)
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    // ========== UPLOAD METADATA ==========
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now(); // ✅ Standardized to Instant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(name = "upload_session_id", length = 100)
    private String uploadSessionId;

    // ========== VISIBILITY & ACCESS ==========
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @Column(name = "access_token", length = 100)
    private String accessToken; // For private file access

    @Column(name = "access_token_expiry")
    private Instant accessTokenExpiry;

    // ========== CONTENT DESCRIPTION ==========
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "alt_text", length = 255)
    private String altText; // For accessibility

    @Column(name = "caption", length = 500)
    private String caption;

    // ========== MEDIA METADATA ==========
    @Column(name = "width")
    private Integer width; // For images/videos in pixels

    @Column(name = "height")
    private Integer height; // For images/videos in pixels

    @Column(name = "duration")
    private Integer duration; // For videos/audio in seconds

    @Column(name = "frame_rate")
    private Double frameRate; // For videos

    @Column(name = "bitrate")
    private Long bitrate; // For audio/video in kbps

    // ========== THUMBNAILS & PREVIEWS ==========
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "preview_url", length = 1000)
    private String previewUrl;

    @Column(name = "compressed_url", length = 1000)
    private String compressedUrl;

    // ========== SECURITY & SCANNING ==========
    @Column(name = "is_scanned", nullable = false)
    private boolean isScanned = false;

    @Column(name = "scan_status", length = 50)
    private String scanStatus; // CLEAN, INFECTED, PENDING, FAILED

    @Column(name = "scan_date")
    private Instant scanDate;

    @Column(name = "virus_signature", length = 100)
    private String virusSignature;

    // ========== RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}