package com.muasamcong.repository;

import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractInfoRepository extends JpaRepository<ContractInfo, Long> {
    Optional<ContractInfo> findByContract(Contract contract);
}
