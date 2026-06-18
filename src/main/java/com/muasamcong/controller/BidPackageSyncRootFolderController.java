package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderCreateRequest;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderCreateResult;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderResult;
import com.muasamcong.service.bidpackage.BidPackageSyncRootFolderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bid-package-sync-root-folders")
@RequiredArgsConstructor
public class BidPackageSyncRootFolderController {
    private final BidPackageSyncRootFolderService service;

    @GetMapping
    public ApiResponse<List<BidPackageSyncRootFolderResult>> list() {
        return ApiResponse.success("Root folders loaded", service.list());
    }

    @PostMapping
    public ApiResponse<BidPackageSyncRootFolderCreateResult> create(@RequestBody BidPackageSyncRootFolderCreateRequest request) {
        return ApiResponse.success("Root folders saved", service.create(request.paths()));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<BidPackageSyncRootFolderResult> activate(@PathVariable Long id) {
        return ApiResponse.success("Root folder activated", service.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<BidPackageSyncRootFolderResult> deactivate(@PathVariable Long id) {
        return ApiResponse.success("Root folder deactivated", service.deactivate(id));
    }
}
