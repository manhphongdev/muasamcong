package com.muasamcong.dto.bidopening;

public record BidOpeningSyncResult(
        String notifyNo,
        Long bidOpeningId,
        int contractorsCreated,
        int contractorsUpdated,
        int contractorsUnchanged,
        int contractorsSkipped
) {
}
