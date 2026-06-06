package com.muasamcong.repository;

import com.muasamcong.model.Investor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorRepository extends JpaRepository<Investor, Long> {
    Optional<Investor> findByInvestorCode(String investorCode);

    @EntityGraph(attributePaths = "taxCodes")
    List<Investor> findByInvestorCodeIn(Collection<String> investorCodes);
}
