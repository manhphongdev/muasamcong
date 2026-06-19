package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;
import com.muasamcong.service.biddingresult.BiddingResultGoodsExcelService;
import com.muasamcong.service.biddingresult.BiddingResultSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidding-results")
@RequiredArgsConstructor
public class BiddingResultSyncController {
    private final BiddingResultSyncService biddingResultSyncService;
    private final BiddingResultGoodsExcelService biddingResultGoodsExcelService;

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BiddingResultSyncResult> syncByNotifyNo(@PathVariable String notifyNo) {
        return ApiResponse.success("Bidding result synced", biddingResultSyncService.syncByNotifyNo(notifyNo));
    }

    @GetMapping("/{notifyNo}/goods/excel")
    public ResponseEntity<byte[]> exportGoodsExcel(@PathVariable String notifyNo) {
        byte[] data = biddingResultGoodsExcelService.exportByNotifyNo(notifyNo);
        String filename = "bid_goods_" + notifyNo + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
