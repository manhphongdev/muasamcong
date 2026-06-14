package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "bidding_documents",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bidding_document_external_id",
                columnNames = {"contract_id", "file_external_id"}
        )
)
public class BiddingDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "file_external_id", nullable = false, length = 100)
    private String fileExternalId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type", length = 32)
    private String fileType;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @Column(name = "file_role", length = 32)
    private String fileRole;

    @Column(name = "storage_path", columnDefinition = "text")
    private String storagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "download_status", nullable = false, length = 20)
    private String downloadStatus;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "downloaded_at")
    private OffsetDateTime downloadedAt;
}
