package com.muasamcong.model;

import com.muasamcong.enums.BidPackageSyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "sync_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_sync_item_notify_no",
                columnNames = "notify_no"
        )
)
public class BidPackageSyncItem extends BaseEntity {

    @Column(name = "notify_no", nullable = false, length = 64)
    private String notifyNo;

    @Column(name = "folder_name")
    private String folderName;

    @Column(name = "source_path", columnDefinition = "text")
    private String sourcePath;

    @Column(name = "source_parent_path", columnDefinition = "text")
    private String sourceParentPath;

    @Column(name = "source_order")
    private Integer sourceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    private BidPackageSyncStatus syncStatus = BidPackageSyncStatus.PENDING;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "last_attempted_at")
    private OffsetDateTime lastAttemptedAt;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;
}
