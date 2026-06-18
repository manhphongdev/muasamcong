package com.muasamcong.repository;

import com.muasamcong.model.BiddingResult;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.Contract;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BiddingResultRepository extends JpaRepository<BiddingResult, Long> {
    Optional<BiddingResult> findByBiddingContractor(BiddingContractor biddingContractor);

    @Query("SELECT br FROM BiddingResult br " +
           "JOIN br.biddingContractor bc " +
           "JOIN bc.bidOpening bo " +
           "WHERE bo.contract IN :contracts AND br.bidResult = 1")
    List<BiddingResult> findWinnersByContracts(@Param("contracts") Collection<Contract> contracts);
}
