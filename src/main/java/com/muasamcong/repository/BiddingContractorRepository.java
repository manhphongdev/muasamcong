package com.muasamcong.repository;

import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.Contractor;
import com.muasamcong.model.Contract;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BiddingContractorRepository extends JpaRepository<BiddingContractor, Long> {
    Optional<BiddingContractor> findByBidOpeningAndContractor(BidOpening bidOpening, Contractor contractor);

    @Query("SELECT bc FROM BiddingContractor bc " +
           "JOIN FETCH bc.contractor " +
           "JOIN bc.bidOpening bo " +
           "WHERE bo.contract IN :contracts")
    List<BiddingContractor> findByContracts(@Param("contracts") Collection<Contract> contracts);
}
