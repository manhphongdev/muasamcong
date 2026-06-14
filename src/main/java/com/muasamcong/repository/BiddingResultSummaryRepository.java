package com.muasamcong.repository;

import com.muasamcong.model.BiddingResultSummary;
import com.muasamcong.model.Contract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingResultSummaryRepository extends JpaRepository<BiddingResultSummary, Long> {
    Optional<BiddingResultSummary> findByContract(Contract contract);
}
