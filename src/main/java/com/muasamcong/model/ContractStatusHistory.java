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
@Table(name = "contract_status_history")
public class ContractStatusHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "from_status", length = 64)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 64)
    private String toStatus;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;
}
