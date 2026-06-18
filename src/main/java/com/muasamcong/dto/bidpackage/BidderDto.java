package com.muasamcong.dto.bidpackage;

import java.math.BigDecimal;

public record BidderDto(
        int no,
        String taxCode,
        String name,
        Long bidPrice,
        BigDecimal discountRate,
        Long finalPrice,
        Integer bidValidityDays,
        String bidSecurity,
        Integer securityValidityDays,
        String executionTimeText
) {}
