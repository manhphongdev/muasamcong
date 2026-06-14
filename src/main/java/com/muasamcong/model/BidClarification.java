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
        name = "bid_clarification",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bid_clarification_contract_req_no",
                columnNames = {"contract_id", "req_no"}
        )
)
public class BidClarification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "notify_version", length = 16)
    private String notifyVersion;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "clarify_req_id", length = 100)
    private String clarifyReqId;

    @Column(name = "req_no", nullable = false, length = 64)
    private String reqNo;

    @Column(name = "req_name", columnDefinition = "text")
    private String reqName;

    @Column(name = "req_date")
    private OffsetDateTime reqDate;

    @Column(name = "sign_req_date")
    private OffsetDateTime signReqDate;

    @Column(name = "sign_res_date")
    private OffsetDateTime signResDate;

    @Column(name = "procuring_entity_code", length = 64)
    private String procuringEntityCode;

    @Column(name = "procuring_entity_name", columnDefinition = "text")
    private String procuringEntityName;

    @Column(name = "portal_created_date")
    private OffsetDateTime portalCreatedDate;
}
