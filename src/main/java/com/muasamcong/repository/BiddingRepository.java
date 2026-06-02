package com.muasamcong.repository;

import com.muasamcong.model.Bidding;
import com.muasamcong.model.ContractInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingRepository extends JpaRepository<Bidding, Long> {
    Optional<Bidding> findByContractInfo(ContractInfo contractInfo);

    boolean existsByContractInfo(ContractInfo contractInfo);
}
