package com.muasamcong.dto.bidopening;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BidOpeningContractorPayload(
        String contractorCode,
        String contractorName,
        Long bidPrice,
        BigDecimal discountRate,
        Long bidPriceAfterDiscount,
        Long bidGuaranteeAmount,
        Integer bidGuaranteeValidityPeriod,
        String contractExecutionTime
) {
}
