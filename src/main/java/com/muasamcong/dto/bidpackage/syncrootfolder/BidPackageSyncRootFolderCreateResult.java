package com.muasamcong.dto.bidpackage.syncrootfolder;

import java.util.List;

public record BidPackageSyncRootFolderCreateResult(
        List<BidPackageSyncRootFolderResult> folders,
        List<BidPackageSyncRootFolderDuplicateResult> duplicates
) {
}
