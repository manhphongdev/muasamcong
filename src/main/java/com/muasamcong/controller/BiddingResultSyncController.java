package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;
import com.muasamcong.service.biddingresult.BiddingResultSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidding-results")
@RequiredArgsConstructor
public class BiddingResultSyncController {
    private final BiddingResultSyncService biddingResultSyncService;

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BiddingResultSyncResult> syncByNotifyNo(@PathVariable String notifyNo) {
        return ApiResponse.success("Bidding result synced", biddingResultSyncService.syncByNotifyNo(notifyNo));
    }
}
