package com.muasamcong.service.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.document.ClarificationResult;
import com.muasamcong.dto.document.DocumentDownloadPendingResult;
import com.muasamcong.dto.document.DocumentEnqueueStats;
import com.muasamcong.dto.document.DocumentSummaryResult;
import com.muasamcong.dto.document.PetitionResult;
import com.muasamcong.model.Contract;
import java.util.List;

public interface BiddingDocumentService {
    DocumentEnqueueStats enqueueBiddingResultFiles(Contract contract, JsonNode main);

    DocumentEnqueueStats enqueueClarificationFiles(Contract contract, JsonNode root);

    DocumentEnqueueStats enqueuePetitionFiles(Contract contract, JsonNode root);

    DocumentDownloadPendingResult downloadPending(int limit);

    DocumentDownloadPendingResult downloadPending(Contract contract, int limit);

    DocumentDownloadPendingResult downloadPending(Contract contract, String sourcePath, int limit);

    DocumentSummaryResult summary(Contract contract);

    List<ClarificationResult> clarifications(Contract contract);

    List<PetitionResult> petitions(Contract contract);
}
