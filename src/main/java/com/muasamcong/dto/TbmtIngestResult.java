package com.muasamcong.dto;

import java.time.OffsetDateTime;

public record TbmtIngestResult(
        String notifyNo,
        Long contractId,
        Long contractInfoId,
        Long biddingId,
        boolean createdContractInfo,
        OffsetDateTime bidClosingTime,
        OffsetDateTime bidOpenTime
) {
}
