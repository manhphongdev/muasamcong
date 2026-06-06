package com.muasamcong.repository;

import com.muasamcong.model.BidPackageSyncItem;
import com.muasamcong.enums.BidPackageSyncStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidPackageSyncItemRepository extends JpaRepository<BidPackageSyncItem, Long> {
    Optional<BidPackageSyncItem> findByNotifyNo(String notifyNo);

    List<BidPackageSyncItem> findByNotifyNoIn(Collection<String> notifyNos);

    List<BidPackageSyncItem> findBySyncStatusInOrderByCreatedAtAsc(
            Collection<BidPackageSyncStatus> syncStatuses,
            Pageable pageable
    );

    List<BidPackageSyncItem> findBySyncStatusOrderByLastSyncedAtAsc(
            BidPackageSyncStatus syncStatus,
            Pageable pageable
    );
}
