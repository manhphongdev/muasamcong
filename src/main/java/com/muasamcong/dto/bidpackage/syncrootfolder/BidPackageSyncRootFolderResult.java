package com.muasamcong.dto.bidpackage.syncrootfolder;

import java.time.OffsetDateTime;

public record BidPackageSyncRootFolderResult(
        Long id,
        String path,
        String status,
        OffsetDateTime lastImportedAt,
        String lastStatus,
        String lastError,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
