package com.muasamcong.repository;

import com.muasamcong.model.BidOpening;
import com.muasamcong.model.ContractInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidOpeningRepository extends JpaRepository<BidOpening, Long> {
    Optional<BidOpening> findByContractInfo(ContractInfo contractInfo);
}
