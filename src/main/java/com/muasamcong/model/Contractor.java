package com.muasamcong.model;

import com.muasamcong.enums.OperatingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contractor")
public class Contractor extends BaseEntity {

    @Column(name = "contractor_code", nullable = false, unique = true, length = 64)
    private String contractorCode;

    @Column(name = "contractor_name", nullable = false)
    private String contractorName;

    @ElementCollection
    @CollectionTable(name = "contractor_tax_codes", joinColumns = @JoinColumn(name = "contractor_id"))
    @Column(name = "tax_code", length = 32)
    private List<String> taxCodes = new ArrayList<>();

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_status", length = 64)
    private OperatingStatus operatingStatus;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;

}
