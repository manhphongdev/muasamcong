package com.muasamcong.repository;

import com.muasamcong.model.BiddingResult;
import com.muasamcong.model.BiddingContractor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingResultRepository extends JpaRepository<BiddingResult, Long> {
    Optional<BiddingResult> findByBiddingContractor(BiddingContractor biddingContractor);
}
