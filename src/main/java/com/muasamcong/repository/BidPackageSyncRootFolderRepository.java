package com.muasamcong.repository;

import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.BidPackageSyncRootFolder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidPackageSyncRootFolderRepository extends JpaRepository<BidPackageSyncRootFolder, Long> {
    List<BidPackageSyncRootFolder> findByStatusOrderByCreatedAtAsc(RecordStatus status);

    List<BidPackageSyncRootFolder> findByStatusInOrderByCreatedAtAsc(Collection<RecordStatus> statuses);

    Optional<BidPackageSyncRootFolder> findByPath(String path);

    boolean existsByStatusIn(Collection<RecordStatus> statuses);
}
