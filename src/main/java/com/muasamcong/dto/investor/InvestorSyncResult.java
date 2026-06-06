package com.muasamcong.dto.investor;

public record InvestorSyncResult(
        long totalElements,
        int totalPages,
        int created,
        int updated,
        int unchanged,
        int failed
) {
}
