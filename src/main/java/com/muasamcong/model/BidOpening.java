package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        name = "bid_opening",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bid_opening_contract",
                columnNames = "contract_id"
        )
)
public class BidOpening extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
}
