package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "sync_source",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_sync_source_path",
                columnNames = "path"
        )
)
public class SyncSource extends BaseEntity {
    @Column(name = "path", nullable = false, columnDefinition = "text")
    private String path;

    @Column(name = "last_imported_at")
    private OffsetDateTime lastImportedAt;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
}
