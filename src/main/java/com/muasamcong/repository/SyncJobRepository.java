package com.muasamcong.repository;

import com.muasamcong.model.SyncJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {
    Optional<SyncJob> findTopByOrderByIdAsc();
}
