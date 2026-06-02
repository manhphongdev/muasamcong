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
@Table(name = "procurement_plan")
public class ProcurementPlan extends BaseEntity {

    @Column(name = "plan_no", nullable = false, unique = true, length = 64)
    private String planNo;

    @Column(name = "plan_name", columnDefinition = "text")
    private String planName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
