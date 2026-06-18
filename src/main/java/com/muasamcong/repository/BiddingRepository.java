package com.muasamcong.repository;

import com.muasamcong.model.Bidding;
import com.muasamcong.model.Contract;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingRepository extends JpaRepository<Bidding, Long> {
    Optional<Bidding> findByContract(Contract contract);

    boolean existsByContract(Contract contract);

    List<Bidding> findByContractIn(Collection<Contract> contracts);
}
