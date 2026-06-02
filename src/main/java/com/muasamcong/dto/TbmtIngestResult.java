package com.muasamcong.dto;

public record TbmtIngestResult(
        String notifyNo,
        Long contractId,
        Long contractInfoId,
        Long biddingId,
        boolean createdContractInfo
) {
}
