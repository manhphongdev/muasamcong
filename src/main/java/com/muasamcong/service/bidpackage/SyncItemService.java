package com.muasamcong.service.bidpackage;

import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageTrackingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SyncItemService {
    BidPackageSyncPendingResult syncPending();

    BidPackageSyncPendingResult refreshSuccess();

    BidPackageSyncPendingItemResult syncByNotifyNo(String notifyNo);

    Page<BidPackageTrackingDto> searchTracking(String search, String status, String kpiFilter, Pageable pageable);

    void updateFolderPath(String notifyNo, String folderPath);
}
