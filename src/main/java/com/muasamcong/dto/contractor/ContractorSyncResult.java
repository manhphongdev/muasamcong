package com.muasamcong.dto.contractor;

public record ContractorSyncResult(
        long totalElements,
        int totalPages,
        int created,
        int updated,
        int unchanged,
        int failed
) {
}
