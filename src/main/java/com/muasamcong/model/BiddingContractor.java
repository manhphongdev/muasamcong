package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "bidding_contractor",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bidding_contractor",
                columnNames = {"bid_opening_id", "contractor_id"}
        )
)
public class BiddingContractor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bid_opening_id", nullable = false)
    private BidOpening bidOpening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;

    @Column(name = "bid_price")
    private Long bidPrice;

    @Column(name = "discount_rate", precision = 8, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "bid_price_after_discount")
    private Long bidPriceAfterDiscount;

    @Column(name = "bid_validity_period")
    private Integer bidValidityPeriod;

    @Column(name = "bid_guarantee_value")
    private Long bidGuaranteeValue;

    @Column(name = "bid_guarantee_validity_period")
    private Integer bidGuaranteeValidityPeriod;

    @Column(name = "contract_execution_time")
    private String contractExecutionTime;
}
