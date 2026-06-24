package com.muasamcong.service.bidpackage;

import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemRunResult;

public interface SyncJobService {
    BidPackageSyncSystemResult getConfig();

    BidPackageSyncSystemRunResult runScheduledIfDue();
}
