package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.PageResponse;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.BidPackageTrackingDto;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import com.muasamcong.service.bidpackage.BidPackageSyncQueueService;
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
    private final BidPackageSyncQueueService bidPackageSyncQueueService;


    @PostMapping("/import-folders")
    public ApiResponse<BidPackageFolderImportResult> importFolders(
            @RequestBody BidPackageFolderImportRequest request
    ) {
        return ApiResponse.success("Bid package folders imported", bidPackageImportService.importFolders(request));
    }

    @PostMapping("/sync-pending")
    public ApiResponse<BidPackageSyncPendingResult> syncPending(
            @RequestParam(defaultValue = "0") int limit
    ) {
        return ApiResponse.success("Pending bid packages synced", bidPackageSyncQueueService.syncPending(limit));
    }

    @PostMapping("/refresh-success")
    public ApiResponse<BidPackageSyncPendingResult> refreshSuccess(
            @RequestParam(defaultValue = "0") int limit
    ) {
        return ApiResponse.success("Successful bid packages refreshed", bidPackageSyncQueueService.refreshSuccess(limit));
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
        Page<BidPackageTrackingDto> trackingPage = bidPackageSyncQueueService.searchTracking(search, status, kpiFilter, pageable);
        return ApiResponse.success(
                "Bid package tracking list fetched",
                PageResponse.from(trackingPage)
        );
    }

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BidPackageSyncPendingItemResult> syncByNotifyNo(
            @org.springframework.web.bind.annotation.PathVariable String notifyNo
    ) {
        return ApiResponse.success("Bid package synced", bidPackageSyncQueueService.syncByNotifyNo(notifyNo));
    }

    @PostMapping("/update-folder")
    public ApiResponse<Void> updateFolder(
            @RequestParam String notifyNo,
            @RequestParam(required = false) String folderPath
    ) {
        bidPackageSyncQueueService.updateFolderPath(notifyNo, folderPath);
        return ApiResponse.success("Folder path updated", null);
    }

    @PostMapping("/open-folder")
    public ApiResponse<Void> openFolder(
            @RequestParam String folderPath
    ) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return ApiResponse.error("Đường dẫn thư mục trống!", null);
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd.exe", "/c", "start", "", folderPath.trim()).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", folderPath.trim()).start();
            } else {
                new ProcessBuilder("xdg-open", folderPath.trim()).start();
            }
            return ApiResponse.success("Đã mở thư mục thành công!", null);
        } catch (Exception e) {
            return ApiResponse.error("Không thể mở thư mục: " + e.getMessage(), null);
        }
    }
}
