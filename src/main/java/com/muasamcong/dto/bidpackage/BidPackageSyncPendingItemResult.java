package com.muasamcong.dto.bidpackage;

public record BidPackageSyncPendingItemResult(
        String notifyNo,
        boolean success,
        String message,
        Long syncItemId,
        Long contractId,
        Long contractInfoId,
        Integer documentFoundThisRun,
        Integer documentNewThisRun,
        Integer documentExistingThisRun,
        Integer documentTotal,
        Integer documentSuccess,
        Integer documentFailed,
        Double documentSuccessRate
) {
}
