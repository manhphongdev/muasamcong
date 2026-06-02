package com.muasamcong.repository;

import com.muasamcong.model.Investor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorRepository extends JpaRepository<Investor, Long> {
    Optional<Investor> findByInvestorCode(String investorCode);
}
