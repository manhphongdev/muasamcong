package com.muasamcong.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record TbmtPayload(
        String notifyVersion,
        String businessStatus,
        String bidName,
        String capitalDetail,
        String investField,
        String bidForm,
        String contractType,
        String bidMode,
        Integer contractPeriod,
        String contractPeriodUnit,
        Boolean multiLot,
        Boolean domestic,
        Long bidPrice,
        String bidPriceUnit,
        Long bidEstimatePrice,
        Integer bidValidityPeriod,
        String bidValidityPeriodUnit,
        Boolean prequalification,
        Boolean internet,
        String submissionMethod,
        String issueLocation,
        String receiveLocation,
        String executionLocation,
        String feeType,
        BigDecimal feeValue,
        String feeUnit,
        OffsetDateTime bidCloseAt,
        OffsetDateTime bidOpenAt,
        String bidOpenLocation,
        BigDecimal guaranteeValue,
        String guaranteeUnit,
        String guaranteeForm,
        OffsetDateTime bidOpeningCompletedAt,
        String investorCode,
        String investorName,
        String oldInvestorName,
        OffsetDateTime mergeInvestorDate,
        String planNo,
        String planName
) {
}
