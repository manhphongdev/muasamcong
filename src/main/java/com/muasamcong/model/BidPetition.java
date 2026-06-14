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
        name = "bid_petition",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bid_petition_contract_req_no",
                columnNames = {"contract_id", "req_no"}
        )
)
public class BidPetition extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "notify_version", length = 16)
    private String notifyVersion;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "req_no", nullable = false, length = 64)
    private String reqNo;

    @Column(name = "req_version", length = 16)
    private String reqVersion;

    @Column(name = "req_name", columnDefinition = "text")
    private String reqName;

    @Column(name = "req_date")
    private OffsetDateTime reqDate;

    @Column(name = "res_date")
    private OffsetDateTime resDate;

    @Column(name = "petition_status", length = 32)
    private String petitionStatus;

    @Column(name = "petition_period", length = 64)
    private String petitionPeriod;

    @Column(name = "contractor_code", length = 64)
    private String contractorCode;

    @Column(name = "contractor_name", columnDefinition = "text")
    private String contractorName;

    @Column(name = "procuring_entity_code", length = 64)
    private String procuringEntityCode;

    @Column(name = "procuring_entity_name", columnDefinition = "text")
    private String procuringEntityName;

    @Column(name = "portal_created_date")
    private OffsetDateTime portalCreatedDate;
}
