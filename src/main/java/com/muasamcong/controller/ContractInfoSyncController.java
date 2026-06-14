package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.ContractInfoSyncResult;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.service.ingest.ContractInfoSyncService;
import com.muasamcong.service.ingest.TbmtSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contract-info")
@RequiredArgsConstructor
public class ContractInfoSyncController {
    private final ContractInfoSyncService contractInfoSyncService;
    private final TbmtSyncService tbmtSyncService;

    @PostMapping("/sync-from-contracts")
    public ApiResponse<ContractInfoSyncResult> syncFromContracts() {
        return ApiResponse.success("Contract information synced", contractInfoSyncService.syncFromContracts());
    }

    @PostMapping({"/sync/{notifyNo}", "/ingest/{notifyNo}"})
    public ApiResponse<TbmtIngestResult> syncOne(@PathVariable String notifyNo) {
        return ApiResponse.success("TBMT synced", tbmtSyncService.syncByNotifyNo(notifyNo));
    }
}
