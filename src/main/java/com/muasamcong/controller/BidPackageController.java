package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.PageResponse;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.BidPackageTrackingDto;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import com.muasamcong.service.bidpackage.SyncItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final SyncItemService syncItemService;


    @PostMapping("/import-folders")
    public ApiResponse<BidPackageFolderImportResult> importFolders(
            @RequestBody BidPackageFolderImportRequest request
    ) {
        return ApiResponse.success("Bid package folders imported", bidPackageImportService.importFolders(request));
    }

    @PostMapping("/sync-pending")
    public ApiResponse<BidPackageSyncPendingResult> syncPending() {
        return ApiResponse.success("Pending bid packages synced", syncItemService.syncPending());
    }

    @PostMapping("/refresh-success")
    public ApiResponse<BidPackageSyncPendingResult> refreshSuccess() {
        return ApiResponse.success("Successful bid packages refreshed", syncItemService.refreshSuccess());
    }

    @GetMapping("/tracking")
    public ApiResponse<PageResponse<BidPackageTrackingDto>> tracking(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kpiFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<BidPackageTrackingDto> trackingPage = syncItemService.searchTracking(search, status, kpiFilter, pageable);
        return ApiResponse.success(
                "Bid package tracking list fetched",
                PageResponse.from(trackingPage)
        );
    }

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BidPackageSyncPendingItemResult> syncByNotifyNo(
            @org.springframework.web.bind.annotation.PathVariable String notifyNo
    ) {
        return ApiResponse.success("Bid package synced", syncItemService.syncByNotifyNo(notifyNo));
    }

    @PostMapping("/update-folder")
    public ApiResponse<Void> updateFolder(
            @RequestParam String notifyNo,
            @RequestParam(required = false) String folderPath
    ) {
        syncItemService.updateFolderPath(notifyNo, folderPath);
        return ApiResponse.success("Folder path updated", null);
    }

}
