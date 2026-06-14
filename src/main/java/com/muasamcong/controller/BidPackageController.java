package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import com.muasamcong.service.bidpackage.BidPackageSyncQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bid-packages")
@RequiredArgsConstructor
public class BidPackageController {
    private final BidPackageImportService bidPackageImportService;
    private final BidPackageSyncQueueService bidPackageSyncQueueService;

    @PostMapping("/import-folders")
    public ApiResponse<BidPackageFolderImportResult> importFolders(
            @RequestBody BidPackageFolderImportRequest request
    ) {
        return ApiResponse.success("Bid package folders imported", bidPackageImportService.importFolders(request));
    }

    @PostMapping("/sync-pending")
    public ApiResponse<BidPackageSyncPendingResult> syncPending(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success("Pending bid packages synced", bidPackageSyncQueueService.syncPending(limit));
    }

    @PostMapping("/refresh-success")
    public ApiResponse<BidPackageSyncPendingResult> refreshSuccess(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success("Successful bid packages refreshed", bidPackageSyncQueueService.refreshSuccess(limit));
    }

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BidPackageSyncPendingItemResult> syncByNotifyNo(
            @org.springframework.web.bind.annotation.PathVariable String notifyNo
    ) {
        return ApiResponse.success("Bid package synced", bidPackageSyncQueueService.syncByNotifyNo(notifyNo));
    }
}
