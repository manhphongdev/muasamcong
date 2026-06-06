package com.muasamcong.dto.biddingresult;

public record BiddingResultSyncResult(
        String notifyNo,
        Long summaryId,
        int resultsCreated,
        int resultsUpdated,
        int resultsUnchanged,
        int resultsSkipped
) {
}
