package com.muasamcong.service.bidpackage.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.PortalSyncContext;
import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.document.DocumentEnqueueStats;
import com.muasamcong.dto.document.DocumentSummaryResult;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.enums.BidStatus;
import com.muasamcong.integration.portal.PortalDocument;
import com.muasamcong.integration.portal.PortalSearch;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractStatusHistory;
import com.muasamcong.model.ProcurementPlan;
import com.muasamcong.model.SyncItem;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.ContractStatusHistoryRepository;
import com.muasamcong.repository.ProcurementPlanRepository;
import com.muasamcong.repository.SyncItemRepository;
import com.muasamcong.service.bidopening.BidOpeningSyncService;
import com.muasamcong.service.biddingresult.BiddingResultSyncService;
import com.muasamcong.service.document.BiddingDocumentService;
import com.muasamcong.service.ingest.TbmtSyncService;
import com.muasamcong.service.monitor.BidStatusResolver;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidPackageSyncPipeline {
    private static final String SOURCE_SYNC_PACKAGE = "SYNC_PACKAGE";

    private final SyncItemRepository syncItemRepository;
    private final ProcurementPlanRepository procurementPlanRepository;
    private final ContractRepository contractRepository;
    private final ContractStatusHistoryRepository contractStatusHistoryRepository;
    private final PortalSearch portalSearchClient;
    private final TbmtSyncService tbmtSyncService;
    private final BidOpeningSyncService bidOpeningSyncService;
    private final BiddingResultSyncService biddingResultSyncService;
    private final PortalDocument portalDocumentClient;
    private final BiddingDocumentService biddingDocumentService;
    private final BidStatusResolver bidStatusResolver;

    public BidPackageSyncPendingItemResult sync(SyncItem item) {
        String notifyNo = item.getNotifyNo();
        item.setSyncStatus(BidPackageSyncStatus.PROCESSING);
        item.setLastAttemptedAt(OffsetDateTime.now());
        item.setLastError(null);
        syncItemRepository.save(item);

        try {
            ResolvedBidDetail resolved = portalSearchClient.resolve(notifyNo)
                    .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + notifyNo));
            PortalSyncContext context = PortalSyncContext.from(notifyNo, resolved);

            String planNo = normalize(context.apiParams().planNo());
            if (planNo == null) {
                throw new IllegalStateException("Cannot resolve planNo for notifyNo: " + notifyNo);
            }

            ProcurementPlan procurementPlan = procurementPlanRepository.findByPlanNo(planNo)
                    .orElseGet(() -> createProcurementPlan(planNo));
            String bidUrl = resolved.detailUrl();
            Contract contract = contractRepository.findByNotifyNo(notifyNo)
                    .orElseGet(() -> createContract(notifyNo, procurementPlan, bidUrl));
            contract.setBidUrl(bidUrl);
            item.setContract(contract);

            TbmtIngestResult ingestResult = tbmtSyncService.sync(context);
            syncBidOpeningIfAvailable(context);
            BiddingResultSyncResult biddingResult = syncBiddingResultIfAvailable(context);
            boolean hasContractorSelectionResult = biddingResult != null && biddingResult.hasContractorSelectionResult();
            DocumentEnqueueStats documentEnqueueStats = statsFrom(biddingResult).plus(syncDocumentFiles(contract, context.apiParams()));
            DocumentSummaryResult documentSummary = biddingDocumentService.summary(contract);
            BidStatus bidStatus = bidStatusResolver.resolveStatus(
                    false,
                    hasContractorSelectionResult,
                    ingestResult.bidClosingTime(),
                    ingestResult.bidOpenTime()
            );

            updateBidStatus(contract, bidStatus, SOURCE_SYNC_PACKAGE);
            item.setSyncStatus(BidPackageSyncStatus.SUCCESS);
            item.setLastSyncedAt(OffsetDateTime.now());
            item.setLastError(null);
            syncItemRepository.save(item);
            log.info("Sync bid package metadata done notifyNo={}, documentFound={}, documentCreated={}, documentExisting={}",
                    notifyNo,
                    documentEnqueueStats.found(),
                    documentEnqueueStats.created(),
                    documentEnqueueStats.existing());

            return new BidPackageSyncPendingItemResult(
                    notifyNo,
                    true,
                    "Synced",
                    item.getId(),
                    contract.getId(),
                    ingestResult.contractInfoId(),
                    documentEnqueueStats.found(),
                    documentEnqueueStats.created(),
                    documentEnqueueStats.existing(),
                    documentSummary.total(),
                    documentSummary.success(),
                    documentSummary.failed(),
                    documentSummary.successRate()
            );
        } catch (Exception ex) {
            item.setSyncStatus(BidPackageSyncStatus.FAILED);
            item.setLastError(ex.getMessage());
            syncItemRepository.save(item);
            log.warn("Sync bid package failed notifyNo={}, error={}", notifyNo, ex.getMessage());

            return new BidPackageSyncPendingItemResult(
                    notifyNo,
                    false,
                    ex.getMessage(),
                    item.getId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private void updateBidStatus(Contract contract, BidStatus newStatus, String source) {
        BidStatus oldStatus = contract.getBidStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            return;
        }

        contract.setBidStatus(newStatus);
        contractRepository.save(contract);

        ContractStatusHistory history = new ContractStatusHistory();
        history.setContract(contract);
        history.setFromStatus(oldStatus == null ? null : oldStatus.name());
        history.setToStatus(newStatus.name());
        history.setSource(source);
        history.setChangedAt(OffsetDateTime.now());
        contractStatusHistoryRepository.save(history);
    }

    private void syncBidOpeningIfAvailable(PortalSyncContext context) {
        String notifyNo = context.notifyNo();
        try {
            bidOpeningSyncService.sync(context);
        } catch (IllegalStateException ex) {
            if (!isUnavailableBidOpening(ex)) {
                throw ex;
            }
            log.info("Skip bid opening sync notifyNo={}, reason={}", notifyNo, ex.getMessage());
        }
    }

    private BiddingResultSyncResult syncBiddingResultIfAvailable(PortalSyncContext context) {
        String notifyNo = context.notifyNo();
        try {
            return biddingResultSyncService.sync(context);
        } catch (IllegalStateException ex) {
            if (!isUnavailableBiddingResult(ex)) {
                throw ex;
            }
            log.info("Skip bidding result sync notifyNo={}, reason={}", notifyNo, ex.getMessage());
            return null;
        }
    }

    private DocumentEnqueueStats statsFrom(BiddingResultSyncResult result) {
        if (result == null) {
            return DocumentEnqueueStats.empty();
        }
        return new DocumentEnqueueStats(
                result.documentFound(),
                result.documentCreated(),
                result.documentExisting()
        );
    }

    private DocumentEnqueueStats syncDocumentFiles(Contract contract, BidApiParams params) {
        return syncClarificationFiles(contract, params).plus(syncPetitionFiles(contract, params));
    }

    private DocumentEnqueueStats syncClarificationFiles(Contract contract, BidApiParams params) {
        try {
            JsonNode root = portalDocumentClient.fetchClarifications(contract.getNotifyNo(), params.processApply());
            DocumentEnqueueStats stats = biddingDocumentService.enqueueClarificationFiles(contract, root);
            log.info("Sync clarification documents done notifyNo={}, found={}, created={}, existing={}",
                    contract.getNotifyNo(), stats.found(), stats.created(), stats.existing());
            return stats;
        } catch (Exception ex) {
            log.warn("Sync clarification documents failed notifyNo={}, error={}", contract.getNotifyNo(), ex.getMessage());
            return DocumentEnqueueStats.empty();
        }
    }

    private DocumentEnqueueStats syncPetitionFiles(Contract contract, BidApiParams params) {
        try {
            JsonNode root = portalDocumentClient.fetchPetitions(contract.getNotifyNo(), params.processApply());
            DocumentEnqueueStats stats = biddingDocumentService.enqueuePetitionFiles(contract, root);
            log.info("Sync petition documents done notifyNo={}, found={}, created={}, existing={}",
                    contract.getNotifyNo(), stats.found(), stats.created(), stats.existing());
            return stats;
        } catch (Exception ex) {
            log.warn("Sync petition documents failed notifyNo={}, error={}", contract.getNotifyNo(), ex.getMessage());
            return DocumentEnqueueStats.empty();
        }
    }

    private boolean isUnavailableBidOpening(IllegalStateException ex) {
        String message = ex.getMessage();
        return message != null && message.startsWith("Cannot resolve bid opening params:");
    }

    private boolean isUnavailableBiddingResult(IllegalStateException ex) {
        String message = ex.getMessage();
        return message != null && message.startsWith("Cannot resolve inputResultId:");
    }

    private ProcurementPlan createProcurementPlan(String planNo) {
        ProcurementPlan procurementPlan = new ProcurementPlan();
        procurementPlan.setPlanNo(planNo);
        procurementPlan.setFetchedAt(OffsetDateTime.now());
        return procurementPlanRepository.save(procurementPlan);
    }

    private Contract createContract(String notifyNo, ProcurementPlan procurementPlan, String bidUrl) {
        Contract contract = new Contract();
        contract.setNotifyNo(notifyNo);
        contract.setBidUrl(bidUrl);
        contract.setProcurementPlan(procurementPlan);
        return contractRepository.save(contract);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
