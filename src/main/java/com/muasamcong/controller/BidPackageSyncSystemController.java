package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemResult;
import com.muasamcong.service.bidpackage.SyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bid-package-sync-system")
@RequiredArgsConstructor
@Slf4j
public class BidPackageSyncSystemController {
    private final SyncJobService service;

    @GetMapping
    public ApiResponse<BidPackageSyncSystemResult> getConfig() {
        log.info("Load bid package sync system config");
        return ApiResponse.success("Bid package sync system config loaded", service.getConfig());
    }
}
