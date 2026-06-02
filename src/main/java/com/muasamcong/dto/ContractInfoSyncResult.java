package com.muasamcong.dto;

public record ContractInfoSyncResult(
        int totalContracts,
        int synced,
        int failed
) {
}
