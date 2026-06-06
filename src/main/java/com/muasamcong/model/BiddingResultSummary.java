package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        name = "bidding_result_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bidding_result_summary_contract_info",
                columnNames = "contract_info_id"
        )
)
public class BiddingResultSummary extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_info_id", nullable = false)
    private ContractInfo contractInfo;

    @Column(name = "result_version", length = 16)
    private String resultVersion;

    @Column(name = "notify_version", length = 16)
    private String notifyVersion;

    @Column(name = "result_status", length = 64)
    private String resultStatus;

    @Column(name = "public_date")
    private OffsetDateTime publicDate;

    @Column(name = "decision_no")
    private String decisionNo;

    @Column(name = "decision_date")
    private OffsetDateTime decisionDate;

    @Column(name = "decision_agency", columnDefinition = "text")
    private String decisionAgency;

    @Column(name = "decision_file_id", length = 100)
    private String decisionFileId;

    @Column(name = "decision_file_name")
    private String decisionFileName;

    @Column(name = "eval_report_file_info", columnDefinition = "text")
    private String evalReportFileInfo;

    @Column(name = "has_winner")
    private Boolean hasWinner;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
