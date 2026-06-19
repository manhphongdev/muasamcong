package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.bidopening.BidOpeningSyncResult;
import com.muasamcong.service.bidopening.BidOpeningPdfService;
import com.muasamcong.service.bidopening.BidOpeningSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bid-openings")
@RequiredArgsConstructor
public class BidOpeningSyncController {
    private final BidOpeningSyncService bidOpeningSyncService;
    private final BidOpeningPdfService bidOpeningPdfService;

    @PostMapping("/sync/{notifyNo}")
    public ApiResponse<BidOpeningSyncResult> syncByNotifyNo(@PathVariable String notifyNo) {
        return ApiResponse.success("Bid opening synced", bidOpeningSyncService.syncByNotifyNo(notifyNo));
    }

    @GetMapping("/{notifyNo}/pdf")
    public ResponseEntity<byte[]> pdfByNotifyNo(@PathVariable String notifyNo) {
        byte[] pdf = bidOpeningPdfService.renderByNotifyNo(notifyNo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("bid-opening-" + notifyNo + ".pdf")
                        .build()
                        .toString())
                .body(pdf);
    }
}
