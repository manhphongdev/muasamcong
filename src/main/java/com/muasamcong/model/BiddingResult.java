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
        name = "bidding_result",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bidding_result_bidding_contractor",
                columnNames = "bidding_contractor_id"
        )
)
public class BiddingResult extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidding_contractor_id", nullable = false)
    private BiddingContractor biddingContractor;

    @Column(name = "bid_result")
    private Integer bidResult;

    @Column(name = "winning_price")
    private Long winningPrice;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "lot_price")
    private Long lotPrice;

    @Column(name = "lot_final_price")
    private Long lotFinalPrice;

    @Column(name = "adjusted_price")
    private Long adjustedPrice;

    @Column(name = "eval_price")
    private Long evalPrice;

    @Column(name = "tech_score", precision = 12, scale = 4)
    private BigDecimal techScore;

    @Column(name = "discount_rate", precision = 8, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "contract_period")
    private Integer contractPeriod;

    @Column(name = "contract_period_unit", length = 16)
    private String contractPeriodUnit;

    @Column(name = "contract_period_text", columnDefinition = "text")
    private String contractPeriodText;

    @Column(name = "contract_execution_time", columnDefinition = "text")
    private String contractExecutionTime;

    @Column(name = "other_content", columnDefinition = "text")
    private String otherContent;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
