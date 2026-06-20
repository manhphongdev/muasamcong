package com.muasamcong.service.bidpackage;

import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemRunResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemUpdateRequest;

public interface SyncJobService {
    BidPackageSyncSystemResult getConfig();

    BidPackageSyncSystemResult updateConfig(BidPackageSyncSystemUpdateRequest request);

    BidPackageSyncSystemRunResult runScheduledIfDue();
}
