package com.muasamcong.dto.bidpackage;

public record BidPackageSyncPendingItemResult(
        String notifyNo,
        boolean success,
        String message,
        Long syncItemId,
        Long contractId,
        Long contractInfoId
) {
}
