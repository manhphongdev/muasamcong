package com.muasamcong.repository;

import com.muasamcong.model.BidPackageSyncSystem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidPackageSyncSystemRepository extends JpaRepository<BidPackageSyncSystem, Long> {
    Optional<BidPackageSyncSystem> findTopByOrderByIdAsc();
}
