package com.muasamcong.dto.biddingresult;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record BiddingResultSummaryPayload(
        String resultVersion,
        String notifyVersion,
        String resultStatus,
        OffsetDateTime publicDate,
        String decisionNo,
        OffsetDateTime decisionDate,
        String decisionAgency,
        String decisionFileId,
        String decisionFileName,
        String evalReportFileInfo,
        boolean hasWinner
) {
}
