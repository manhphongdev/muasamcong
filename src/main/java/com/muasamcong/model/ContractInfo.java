package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "contract_info",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_contract_info_contract_version",
                columnNames = {"contract_id", "version"}
        )
)
public class ContractInfo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    private String businessStatus;

    @Column(name = "version", length = 16)
    private String version;

    @Column(name = "bid_name", columnDefinition = "text")
    private String bidName;

    @ManyToOne
    @JoinColumn(name = "investor_id")
    private Investor investor;

    private String capitalDetail;

    private String investField;

    private String bidForm;

    private String contractType;

    private String bidMode;

    private Integer contractPeriod;

    private String contractPeriodUnit;

    private Boolean multiLot;

    private Boolean domestic;

    private Long bidPrice;

    private String bidPriceUnit;

    private Long bidEstimatePrice;

    private Integer bidValidityPeriod;

    private String bidValidityPeriodUnit;

    @Column(name = "is_prequalification")
    private Boolean prequalification;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
