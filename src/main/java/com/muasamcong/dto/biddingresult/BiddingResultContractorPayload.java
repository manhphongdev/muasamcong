package com.muasamcong.dto.biddingresult;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BiddingResultContractorPayload(
        String contractorCode,
        String contractorName,
        String taxCode,
        Integer bidResult,
        Long winningPrice,
        String reason,
        Long lotPrice,
        Long lotFinalPrice,
        Long adjustedPrice,
        Long evalPrice,
        BigDecimal techScore,
        BigDecimal discountRate,
        Integer contractPeriod,
        String contractPeriodUnit,
        String contractPeriodText,
        String contractExecutionTime,
        String otherContent
) {
}
