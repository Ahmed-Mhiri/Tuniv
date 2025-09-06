package com.tuniv.backend.qa.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attachments")
@Getter
@Setter
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Integer attachmentId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_type")
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    // ✨ --- THE PERSISTENT FIX: Implement equals() and hashCode() --- ✨
    // This provides a stable way for Hibernate and Java Sets to manage attachments.

    @Override
    public boolean equals(Object o) {
        // 1. Check if it's the exact same object in memory
        if (this == o) return true;
        
        // 2. Check if the other object is null or of a different class
        if (o == null || getClass() != o.getClass()) return false;
        
        // 3. Cast the object to an Attachment
        Attachment that = (Attachment) o;
        
        // 4. Compare by the unique ID. If attachmentId is null, they are not equal.
        return this.attachmentId != null && Objects.equals(this.attachmentId, that.attachmentId);
    }

    @Override
    public int hashCode() {
        // 5. Generate a hash code based on the class and the ID.
        // Using getClass() ensures that subclasses are not considered equal.
        return Objects.hash(getClass(), this.attachmentId);
    }
}
