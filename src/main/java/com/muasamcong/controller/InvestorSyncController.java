package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.investor.InvestorSyncResult;
import com.muasamcong.service.investor.InvestorSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/investors")
@RequiredArgsConstructor
public class InvestorSyncController {
    private final InvestorSyncService investorSyncService;

    @PostMapping("/sync")
    public ApiResponse<InvestorSyncResult> syncInvestors(@RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.success(investorSyncService.syncInvestors(pageSize));
    }
}
