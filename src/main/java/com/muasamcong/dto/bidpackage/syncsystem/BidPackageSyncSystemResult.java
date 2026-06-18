package com.muasamcong.dto.bidpackage.syncsystem;

import java.time.OffsetDateTime;
import java.util.List;

public record BidPackageSyncSystemResult(
        Boolean enabled,
        Integer intervalMinutes,
        Integer batchLimit,
        List<String> importRootPaths,
        OffsetDateTime lastRunAt,
        OffsetDateTime nextRunAt,
        Boolean running,
        String lastStatus,
        String lastError,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer totalItems,
        Integer processedItems,
        Integer successItems,
        Integer failedItems,
        Integer skippedItems,
        String currentNotifyNo
) {
}
