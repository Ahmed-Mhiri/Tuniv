package com.tuniv.backend.shared.model;

import java.time.Instant;

/**
 * Interface defining the contract for entities that support soft deletion.
 * Entities implementing this interface should not be physically deleted but marked as deleted.
 */
public interface SoftDeletable {

    String DELETED_CLAUSE = "is_deleted = false";

    void setDeleted(boolean deleted);
    boolean isDeleted();

    void setDeletedAt(Instant deletedAt);
    Instant getDeletedAt();

    void setDeletionReason(String reason);
    String getDeletionReason();

    /**
     * Performs a soft delete by setting the deleted flag and timestamp.
     * @param reason The reason for deletion (optional).
     */
    default void softDelete(String reason) {
        setDeleted(true);
        setDeletedAt(Instant.now());
        setDeletionReason(reason);
    }

    /**
     * Restores a soft-deleted entity by clearing the deletion markers.
     */
    default void restore() {
        setDeleted(false);
        setDeletedAt(null);
        setDeletionReason(null);
    }
}
