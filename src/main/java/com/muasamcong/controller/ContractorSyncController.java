package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.contractor.ContractorSyncResult;
import com.muasamcong.service.contractor.ContractorSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractors")
@RequiredArgsConstructor
public class ContractorSyncController {
    private final ContractorSyncService contractorSyncService;

    @PostMapping("/sync")
    public ApiResponse<ContractorSyncResult> syncContractors(@RequestParam(defaultValue = "500") int pageSize) {
        return ApiResponse.success(contractorSyncService.syncContractors(pageSize));
    }
}
