package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sync_job")
public class SyncJob extends BaseEntity {
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes = 30;

    @Column(name = "last_run_at")
    private OffsetDateTime lastRunAt;

    @Column(name = "next_run_at")
    private OffsetDateTime nextRunAt;

    @Column(name = "running", nullable = false)
    private Boolean running = false;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems = 0;

    @Column(name = "success_items", nullable = false)
    private Integer successItems = 0;

    @Column(name = "failed_items", nullable = false)
    private Integer failedItems = 0;

}
