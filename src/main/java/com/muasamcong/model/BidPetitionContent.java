package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bid_petition_content")
public class BidPetitionContent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "petition_id", nullable = false)
    private BidPetition petition;

    @Column(name = "petition_period", length = 64)
    private String petitionPeriod;

    @Column(name = "is_decision", length = 16)
    private String decision;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "req_content", columnDefinition = "text")
    private String reqContent;

    @Column(name = "res_content", columnDefinition = "text")
    private String resContent;

    @Column(name = "req_date")
    private OffsetDateTime reqDate;

    @Column(name = "res_date")
    private OffsetDateTime resDate;

    @Column(name = "is_replied")
    private Boolean replied;

    @Column(name = "is_restrict_competition", length = 16)
    private String restrictCompetition;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
