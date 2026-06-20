package com.muasamcong.service.document.impl;

import com.muasamcong.dto.document.DocumentDownloadPendingResult;
import com.muasamcong.service.document.BiddingDocumentService;
import com.muasamcong.service.document.DocumentDownloadWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDownloadWorkerServiceImpl implements DocumentDownloadWorkerService {
    private final BiddingDocumentService biddingDocumentService;

    @Override
    public DocumentDownloadPendingResult downloadPending(int limit) {
        int safeLimit = Math.max(limit, 1);
        log.info("Document download worker start limit={}", safeLimit);
        DocumentDownloadPendingResult result = biddingDocumentService.downloadPending(safeLimit);
        log.info(
                "Document download worker done total={}, success={}, failed={}, skipped={}",
                result.total(),
                result.success(),
                result.failed(),
                result.skipped()
        );
        return result;
    }
}
