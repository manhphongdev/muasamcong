package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "bidding",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bidding_contract",
                columnNames = "contract_id"
        )
)
public class Bidding extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "is_internet")
    private Boolean internet;

    @Column(name = "submission_method", columnDefinition = "text")
    private String submissionMethod;

    @Column(name = "issue_location", columnDefinition = "text")
    private String issueLocation;

    @Column(name = "receive_location", columnDefinition = "text")
    private String receiveLocation;

    @Column(name = "execution_location", columnDefinition = "text")
    private String executionLocation;

    @Column(name = "fee_type", length = 16)
    private String feeType;

    @Column(name = "fee_value", precision = 18, scale = 2)
    private BigDecimal feeValue;

    @Column(name = "fee_unit", length = 8)
    private String feeUnit;

    @Column(name = "bid_close_at")
    private OffsetDateTime bidCloseAt;

    @Column(name = "bid_open_at")
    private OffsetDateTime bidOpenAt;

    @Column(name = "bid_open_location", columnDefinition = "text")
    private String bidOpenLocation;

    @Column(name = "guarantee_value", precision = 18, scale = 2)
    private BigDecimal guaranteeValue;

    @Column(name = "guarantee_unit", length = 8)
    private String guaranteeUnit;

    @Column(name = "guarantee_form", columnDefinition = "text")
    private String guaranteeForm;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
