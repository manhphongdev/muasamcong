package com.muasamcong.model;

import com.muasamcong.enums.BidStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contract")
public class Contract extends BaseEntity {

    private String notifyNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "bid_status", length = 64)
    private BidStatus bidStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProcurementPlan procurementPlan;

}
