package com.muasamcong.repository;

import com.muasamcong.model.BidClarification;
import com.muasamcong.model.Contract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidClarificationRepository extends JpaRepository<BidClarification, Long> {
    Optional<BidClarification> findByContractAndReqNo(Contract contract, String reqNo);

    List<BidClarification> findByContractOrderByReqDateDescCreatedAtDesc(Contract contract);
}
