package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.bidopening.BidOpeningSyncResult;
import com.muasamcong.service.bidopening.BidOpeningSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bid-openings")
@RequiredArgsConstructor
public class BidOpeningSyncController {
    private final BidOpeningSyncService bidOpeningSyncService;

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BidOpeningSyncResult> syncByNotifyNo(@PathVariable String notifyNo) {
        return ApiResponse.success("Bid opening synced", bidOpeningSyncService.syncByNotifyNo(notifyNo));
    }
}
