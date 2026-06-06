package com.muasamcong.dto.bidpackage;

import java.util.List;

public record BidPackageSyncPendingResult(
        int total,
        int success,
        int failed,
        List<BidPackageSyncPendingItemResult> items
) {
}
