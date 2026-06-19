package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bidding_result_goods")
public class BiddingResultGoods extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_summary_id")
    private BiddingResultSummary resultSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_result_id")
    private BiddingResult biddingResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    @Column(name = "notify_no", length = 64)
    private String notifyNo;

    @Column(name = "bid_name", columnDefinition = "text")
    private String bidName;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "lot_name", columnDefinition = "text")
    private String lotName;

    @Column(name = "contractor_code", length = 64)
    private String contractorCode;

    @Column(name = "goods_name", columnDefinition = "text")
    private String goodsName;

    @Column(name = "goods_code", columnDefinition = "text")
    private String goodsCode;

    @Column(name = "goods_label", columnDefinition = "text")
    private String goodsLabel;

    @Column(name = "year_manufacture", columnDefinition = "text")
    private String yearManufacture;

    @Column(name = "origin", columnDefinition = "text")
    private String origin;

    @Column(name = "manufacturer", columnDefinition = "text")
    private String manufacturer;

    @Column(name = "technical_features", columnDefinition = "text")
    private String technicalFeatures;

    @Column(name = "unit", length = 100)
    private String unit;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "hs_code", length = 100)
    private String hsCode;

    @Column(name = "winning_unit_price")
    private Long winningUnitPrice;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "delivery_time", columnDefinition = "text")
    private String deliveryTime;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "raw_item", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rawItem;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
