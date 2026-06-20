package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemUpdateRequest;
import com.muasamcong.service.bidpackage.SyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PutMapping
    public ApiResponse<BidPackageSyncSystemResult> updateConfig(@RequestBody BidPackageSyncSystemUpdateRequest request) {
        log.info("Update bid package sync system config enabled={}, intervalMinutes={}, importRootPaths={}",
                request.enabled(), request.intervalMinutes(),
                request.importRootPaths() == null ? 0 : request.importRootPaths().size());
        return ApiResponse.success("Bid package sync system config updated", service.updateConfig(request));
    }

}
