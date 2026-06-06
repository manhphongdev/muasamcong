package com.muasamcong.repository;

import com.muasamcong.model.Contractor;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractorRepository extends JpaRepository<Contractor, Long> {
    @EntityGraph(attributePaths = "taxCodes")
    List<Contractor> findByContractorCodeIn(Collection<String> contractorCodes);
}
