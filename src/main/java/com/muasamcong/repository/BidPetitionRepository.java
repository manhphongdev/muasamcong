package com.muasamcong.repository;

import com.muasamcong.model.BidPetition;
import com.muasamcong.model.Contract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidPetitionRepository extends JpaRepository<BidPetition, Long> {
    Optional<BidPetition> findByContractAndReqNo(Contract contract, String reqNo);

    List<BidPetition> findByContractOrderByReqDateDescCreatedAtDesc(Contract contract);
}
