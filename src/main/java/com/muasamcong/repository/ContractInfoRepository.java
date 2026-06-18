package com.muasamcong.repository;

import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractInfoRepository extends JpaRepository<ContractInfo, Long> {
    Optional<ContractInfo> findByContractAndStatus(Contract contract, RecordStatus status);

    Optional<ContractInfo> findByContractAndVersion(Contract contract, String version);

    List<ContractInfo> findByContractInAndStatus(Collection<Contract> contracts, RecordStatus status);
}
