package com.muasamcong.dto.bidpackage.syncsystem;

import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;

public record BidPackageSyncSystemRunResult(
        boolean skipped,
        String message,
        BidPackageFolderImportResult importResult,
        BidPackageSyncPendingResult syncPendingResult,
        BidPackageSyncPendingResult refreshSuccessResult,
        BidPackageSyncSystemResult config
) {
}
