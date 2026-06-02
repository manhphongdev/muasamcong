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
                columnNames = {"contract_info_id", "file_external_id"}
        )
)
public class BiddingDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_info_id", nullable = false)
    private ContractInfo contractInfo;

    @Column(name = "file_external_id", nullable = false, length = 100)
    private String fileExternalId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "file_type", length = 32)
    private String fileType;

    @Column(name = "storage_path", columnDefinition = "text")
    private String storagePath;

    @Column(name = "import_status", nullable = false, length = 20)
    private String importStatus;

    @Column(name = "imported_at")
    private OffsetDateTime importedAt;
}
