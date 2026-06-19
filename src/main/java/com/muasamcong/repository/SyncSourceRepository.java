package com.muasamcong.repository;

import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.SyncSource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncSourceRepository extends JpaRepository<SyncSource, Long> {
    List<SyncSource> findByStatusOrderByCreatedAtAsc(RecordStatus status);

    List<SyncSource> findByStatusInOrderByCreatedAtAsc(Collection<RecordStatus> statuses);

    Optional<SyncSource> findByPath(String path);

    boolean existsByStatusIn(Collection<RecordStatus> statuses);
}
