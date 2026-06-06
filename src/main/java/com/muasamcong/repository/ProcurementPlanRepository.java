package com.muasamcong.repository;

import com.muasamcong.model.ProcurementPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementPlanRepository extends JpaRepository<ProcurementPlan, Long> {
    Optional<ProcurementPlan> findByPlanNo(String planNo);
}
