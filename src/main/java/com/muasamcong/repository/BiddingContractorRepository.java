package com.muasamcong.repository;

import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.Contractor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingContractorRepository extends JpaRepository<BiddingContractor, Long> {
    Optional<BiddingContractor> findByBidOpeningAndContractor(BidOpening bidOpening, Contractor contractor);
}
