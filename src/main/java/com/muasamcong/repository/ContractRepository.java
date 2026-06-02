package com.muasamcong.repository;

import com.muasamcong.model.Contract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByNotifyNo(String notifyNo);

    List<Contract> findByNotifyNoIsNotNull();
}
