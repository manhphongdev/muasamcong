package com.muasamcong.dto.bidpackage.syncsystem;

import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.document.DocumentDownloadPendingResult;
import com.muasamcong.dto.export.AutoDownloadExportResult;

public record BidPackageSyncSystemRunResult(
        boolean skipped,
        String message,
        BidPackageFolderImportResult importResult,
        BidPackageSyncPendingResult syncPendingResult,
        BidPackageSyncPendingResult refreshSuccessResult,
        DocumentDownloadPendingResult documentDownloadResult,
        AutoDownloadExportResult exportResult,
        BidPackageSyncSystemResult config
) {
}
