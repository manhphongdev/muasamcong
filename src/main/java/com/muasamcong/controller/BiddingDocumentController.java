package com.muasamcong.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.document.ClarificationResult;
import com.muasamcong.dto.document.DocumentDownloadPendingResult;
import com.muasamcong.dto.document.DocumentEnqueueResult;
import com.muasamcong.dto.document.DocumentEnqueueStats;
import com.muasamcong.dto.document.DocumentSummaryResult;
import com.muasamcong.dto.document.PetitionResult;
import com.muasamcong.integration.portal.PortalDocument;
import com.muasamcong.integration.portal.PortalSearch;
import com.muasamcong.model.Contract;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.service.document.BiddingDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class BiddingDocumentController {
    private final PortalSearch portalSearchClient;
    private final PortalDocument portalDocumentClient;
    private final ContractRepository contractRepository;
    private final BiddingDocumentService biddingDocumentService;

    @PostMapping("/sync-clarifications/{notifyNo}")
    public ApiResponse<DocumentEnqueueResult> syncClarifications(@PathVariable String notifyNo) {
        Contract contract = contract(notifyNo);
        BidApiParams params = params(notifyNo);
        JsonNode root = portalDocumentClient.fetchClarifications(contract.getNotifyNo(), params.processApply());
        DocumentEnqueueStats stats = biddingDocumentService.enqueueClarificationFiles(contract, root);
        return ApiResponse.success("Clarification documents enqueued", new DocumentEnqueueResult(contract.getNotifyNo(), stats.created(), stats.existing()));
    }

    @PostMapping("/sync-petitions/{notifyNo}")
    public ApiResponse<DocumentEnqueueResult> syncPetitions(@PathVariable String notifyNo) {
        Contract contract = contract(notifyNo);
        BidApiParams params = params(notifyNo);
        JsonNode root = portalDocumentClient.fetchPetitions(contract.getNotifyNo(), params.processApply());
        DocumentEnqueueStats stats = biddingDocumentService.enqueuePetitionFiles(contract, root);
        return ApiResponse.success("Petition documents enqueued", new DocumentEnqueueResult(contract.getNotifyNo(), stats.created(), stats.existing()));
    }

    @PostMapping("/download-pending")
    public ApiResponse<DocumentDownloadPendingResult> downloadPending(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success("Pending documents downloaded", biddingDocumentService.downloadPending(limit));
    }

    @GetMapping("/summary/{notifyNo}")
    public ApiResponse<DocumentSummaryResult> summary(@PathVariable String notifyNo) {
        return ApiResponse.success("Document summary loaded", biddingDocumentService.summary(contract(notifyNo)));
    }

    @GetMapping("/clarifications/{notifyNo}")
    public ApiResponse<List<ClarificationResult>> clarifications(@PathVariable String notifyNo) {
        return ApiResponse.success("Clarifications loaded", biddingDocumentService.clarifications(contract(notifyNo)));
    }

    @GetMapping("/petitions/{notifyNo}")
    public ApiResponse<List<PetitionResult>> petitions(@PathVariable String notifyNo) {
        return ApiResponse.success("Petitions loaded", biddingDocumentService.petitions(contract(notifyNo)));
    }

    private Contract contract(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        return contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));
    }

    private BidApiParams params(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        return portalSearchClient.resolve(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + normalizedNotifyNo))
                .apiParams();
    }

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim().toUpperCase();
    }
}
