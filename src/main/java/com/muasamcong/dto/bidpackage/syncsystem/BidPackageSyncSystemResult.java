package com.muasamcong.dto.bidpackage.syncsystem;

import java.time.OffsetDateTime;
import java.util.List;

public record BidPackageSyncSystemResult(
        Boolean enabled,
        Long fixedDelayMs,
        Integer documentDownloadLimit,
        List<String> importRootPaths,
        OffsetDateTime lastRunAt,
        Boolean running,
        String lastStatus,
        String lastError,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer totalItems,
        Integer successItems,
        Integer failedItems
) {
}
