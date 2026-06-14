package com.muasamcong.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "bid_clarification_content",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bid_clarification_content_external_id",
                columnNames = {"clarification_id", "external_id"}
        )
)
public class BidClarificationContent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clarification_id", nullable = false)
    private BidClarification clarification;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "subject_code", length = 64)
    private String subjectCode;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "question", columnDefinition = "text")
    private String question;

    @Column(name = "response", columnDefinition = "text")
    private String response;

    @Column(name = "cat_type", length = 64)
    private String catType;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
