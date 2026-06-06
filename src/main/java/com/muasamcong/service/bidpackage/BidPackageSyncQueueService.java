package com.muasamcong.service.bidpackage;

import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;

public interface BidPackageSyncQueueService {
    BidPackageSyncPendingResult syncPending(int limit);

    BidPackageSyncPendingResult refreshSuccess(int limit);

    BidPackageSyncPendingItemResult syncByNotifyNo(String notifyNo);
}
