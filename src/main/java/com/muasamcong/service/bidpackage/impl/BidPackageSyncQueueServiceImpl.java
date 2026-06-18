package com.muasamcong.service.bidpackage.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.document.DocumentDownloadPendingResult;
import com.muasamcong.dto.document.DocumentEnqueueStats;
import com.muasamcong.dto.document.DocumentSummaryResult;
import com.muasamcong.enums.BidStatus;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.integration.portal.PortalDocumentClient;
import com.muasamcong.integration.portal.PortalSearchClient;
import com.muasamcong.model.BidPackageSyncItem;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractStatusHistory;
import com.muasamcong.model.ProcurementPlan;
import com.muasamcong.model.Bidding;
import com.muasamcong.model.BiddingDocument;
import com.muasamcong.model.BiddingResult;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.repository.BidPackageSyncItemRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.ContractStatusHistoryRepository;
import com.muasamcong.repository.ProcurementPlanRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.BiddingRepository;
import com.muasamcong.repository.BiddingResultRepository;
import com.muasamcong.repository.BiddingDocumentRepository;
import com.muasamcong.repository.BiddingContractorRepository;
import com.muasamcong.service.bidopening.BidOpeningSyncService;
import com.muasamcong.service.bidpackage.BidPackageSyncQueueService;
import com.muasamcong.service.biddingresult.BiddingResultSyncService;
import com.muasamcong.service.document.BiddingDocumentService;
import com.muasamcong.service.ingest.TbmtSyncService;
import com.muasamcong.service.monitor.BidStatusResolver;
import com.muasamcong.dto.bidpackage.BidderDto;
import com.muasamcong.dto.bidpackage.ScrapingLogDto;
import com.muasamcong.dto.bidpackage.MissingFieldDto;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.dto.bidpackage.BidPackageTrackingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidPackageSyncQueueServiceImpl implements BidPackageSyncQueueService {
    private static final String SOURCE_SYNC_PACKAGE = "SYNC_PACKAGE";

    private final BidPackageSyncItemRepository syncItemRepository;
    private final ProcurementPlanRepository procurementPlanRepository;
    private final ContractRepository contractRepository;
    private final ContractStatusHistoryRepository contractStatusHistoryRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BiddingRepository biddingRepository;
    private final BiddingResultRepository biddingResultRepository;
    private final BiddingDocumentRepository biddingDocumentRepository;
    private final BiddingContractorRepository biddingContractorRepository;
    private final PortalSearchClient portalSearchClient;
    private final TbmtSyncService tbmtSyncService;
    private final BidOpeningSyncService bidOpeningSyncService;
    private final BiddingResultSyncService biddingResultSyncService;
    private final PortalDocumentClient portalDocumentClient;
    private final BiddingDocumentService biddingDocumentService;
    private final BidStatusResolver bidStatusResolver;

    @Override
    public BidPackageSyncPendingResult syncPending(int limit) {
        int safeLimit = normalizeLimit(limit);
        Pageable pageable = safeLimit <= 0 ? Pageable.unpaged() : PageRequest.of(0, safeLimit);
        List<BidPackageSyncItem> items = syncItemRepository.findSyncQueue(
                List.of(BidPackageSyncStatus.PENDING, BidPackageSyncStatus.FAILED),
                pageable
        );

        log.info("Sync bid packages pending start limit={}, items={}", safeLimit <= 0 ? "ALL" : safeLimit, items.size());
        BidPackageSyncPendingResult result = syncItems(items);
        log.info("Sync bid packages pending done success={}, failed={}", result.success(), result.failed());
        return result;
    }

    @Override
    public BidPackageSyncPendingResult refreshSuccess(int limit) {
        int safeLimit = normalizeLimit(limit);
        Pageable pageable = safeLimit <= 0 ? Pageable.unpaged() : PageRequest.of(0, safeLimit);
        List<BidPackageSyncItem> items = syncItemRepository.findRefreshQueue(
                BidPackageSyncStatus.SUCCESS,
                pageable
        );

        log.info("Refresh bid packages success start limit={}, items={}", safeLimit <= 0 ? "ALL" : safeLimit, items.size());
        BidPackageSyncPendingResult result = syncItems(items);
        log.info("Refresh bid packages success done success={}, failed={}", result.success(), result.failed());
        return result;
    }

    private BidPackageSyncPendingResult syncItems(List<BidPackageSyncItem> items) {
        List<BidPackageSyncPendingItemResult> results = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (BidPackageSyncItem item : items) {
            BidPackageSyncPendingItemResult result = syncItem(item);
            results.add(result);
            if (result.success()) {
                success++;
            } else {
                failed++;
            }
        }

        return new BidPackageSyncPendingResult(items.size(), success, failed, results);
    }

    @Override
    public BidPackageSyncPendingItemResult syncByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        BidPackageSyncItem item = syncItemRepository.findByNotifyNo(normalizedNotifyNo).orElseGet(() -> {
            BidPackageSyncItem newItem = new BidPackageSyncItem();
            newItem.setNotifyNo(normalizedNotifyNo);
            newItem.setSyncStatus(BidPackageSyncStatus.PENDING);
            return syncItemRepository.save(newItem);
        });

        return syncItem(item);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<BidPackageTrackingDto> searchTracking(String search, String status, String kpiFilter, Pageable pageable) {
        Page<BidPackageSyncItem> itemPage = syncItemRepository.searchTracking(
                normalizeFilter(search),
                normalizeFilter(status),
                normalizeFilter(kpiFilter),
                OffsetDateTime.now().plusDays(1),
                pageable
        );
        if (itemPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, itemPage.getTotalElements());
        }

        List<String> notifyNos = itemPage.getContent().stream()
                .map(BidPackageSyncItem::getNotifyNo)
                .toList();
        List<Contract> contracts = contractRepository.findByNotifyNoIn(notifyNos);
        Map<String, Contract> contractByNotifyNo = contracts.stream()
                .collect(Collectors.toMap(Contract::getNotifyNo, Function.identity(), (first, second) -> first));
        Map<Long, ContractInfo> infoByContractId = contracts.isEmpty() ? Map.of() : contractInfoRepository.findByContractInAndStatus(contracts, RecordStatus.ACTIVE).stream()
                .collect(Collectors.toMap(info -> info.getContract().getId(), Function.identity(), (first, second) -> first));
        Map<Long, Bidding> biddingByContractId = contracts.isEmpty() ? Map.of() : biddingRepository.findByContractIn(contracts).stream()
                .collect(Collectors.toMap(bidding -> bidding.getContract().getId(), Function.identity(), (first, second) -> first));
        Map<Long, List<BiddingContractor>> biddersByContractId = contracts.isEmpty() ? Map.of() : biddingContractorRepository.findByContracts(contracts).stream()
                .collect(Collectors.groupingBy(bidder -> bidder.getBidOpening().getContract().getId()));
        Map<Long, BiddingResult> winnerByContractId = contracts.isEmpty() ? Map.of() : biddingResultRepository.findWinnersByContracts(contracts).stream()
                .collect(Collectors.toMap(
                        result -> result.getBiddingContractor().getBidOpening().getContract().getId(),
                        Function.identity(),
                        (first, second) -> first
                ));
        Map<Long, List<BiddingDocument>> documentsByContractId = contracts.isEmpty() ? Map.of() : biddingDocumentRepository.findByContractIn(contracts).stream()
                .collect(Collectors.groupingBy(document -> document.getContract().getId()));

        List<BidPackageTrackingDto> content = itemPage.getContent().stream()
                .map(item -> toTrackingDto(
                        item,
                        contractByNotifyNo.get(item.getNotifyNo()),
                        infoByContractId,
                        biddingByContractId,
                        biddersByContractId,
                        winnerByContractId,
                        documentsByContractId
                ))
                .toList();

        return new PageImpl<>(content, pageable, itemPage.getTotalElements());
    }

    private BidPackageSyncPendingItemResult syncItem(BidPackageSyncItem item) {
        String notifyNo = item.getNotifyNo();
        item.setSyncStatus(BidPackageSyncStatus.PROCESSING);
        item.setLastAttemptedAt(OffsetDateTime.now());
        item.setLastError(null);
        syncItemRepository.save(item);

        try {
            ResolvedBidDetail resolved = portalSearchClient.resolve(notifyNo)
                    .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + notifyNo));

            String planNo = normalize(resolved.apiParams().planNo());
            if (planNo == null) {
                throw new IllegalStateException("Cannot resolve planNo for notifyNo: " + notifyNo);
            }

            ProcurementPlan procurementPlan = procurementPlanRepository.findByPlanNo(planNo)
                    .orElseGet(() -> createProcurementPlan(planNo));
            String bidUrl = resolved.detailUrl();
            Contract contract = contractRepository.findByNotifyNo(notifyNo)
                    .orElseGet(() -> createContract(notifyNo, procurementPlan, bidUrl));
            contract.setBidUrl(bidUrl);

            TbmtIngestResult ingestResult = tbmtSyncService.syncByNotifyNo(notifyNo);
            syncBidOpeningIfAvailable(notifyNo);
            BiddingResultSyncResult biddingResult = syncBiddingResultIfAvailable(notifyNo);
            boolean hasContractorSelectionResult = biddingResult != null && biddingResult.hasContractorSelectionResult();
            DocumentEnqueueStats documentEnqueueStats = statsFrom(biddingResult).plus(syncDocumentFiles(contract, resolved.apiParams()));
            DocumentDownloadPendingResult documentDownloadResult = biddingDocumentService.downloadPending(contract, item.getSourcePath(), 50);
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
            log.info("Sync bid package documents notifyNo={}, total={}, success={}, failed={}, skipped={}",
                    notifyNo,
                    documentDownloadResult.total(),
                    documentDownloadResult.success(),
                    documentDownloadResult.failed(),
                    documentDownloadResult.skipped());

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

    private void syncBidOpeningIfAvailable(String notifyNo) {
        try {
            bidOpeningSyncService.syncByNotifyNo(notifyNo);
        } catch (IllegalStateException ex) {
            if (!isUnavailableBidOpening(ex)) {
                throw ex;
            }
            log.info("Skip bid opening sync notifyNo={}, reason={}", notifyNo, ex.getMessage());
        }
    }

    private BiddingResultSyncResult syncBiddingResultIfAvailable(String notifyNo) {
        try {
            return biddingResultSyncService.syncByNotifyNo(notifyNo);
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

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim();
    }

    private int normalizeLimit(int limit) {
        return Math.max(limit, 0);
    }

    private BidPackageTrackingDto toTrackingDto(
            BidPackageSyncItem item,
            Contract contract,
            Map<Long, ContractInfo> infoByContractId,
            Map<Long, Bidding> biddingByContractId,
            Map<Long, List<BiddingContractor>> biddersByContractId,
            Map<Long, BiddingResult> winnerByContractId,
            Map<Long, List<BiddingDocument>> documentsByContractId
    ) {
        ContractInfo info = contract == null ? null : infoByContractId.get(contract.getId());
        Bidding bidding = contract == null ? null : biddingByContractId.get(contract.getId());
        List<BiddingContractor> bidders = contract == null ? List.of() : biddersByContractId.getOrDefault(contract.getId(), List.of());
        BiddingResult winner = contract == null ? null : winnerByContractId.get(contract.getId());
        List<BiddingDocument> documents = contract == null ? List.of() : documentsByContractId.getOrDefault(contract.getId(), List.of());
        List<BidderDto> bidderDtos = toBidderDtos(bidders);

        return new BidPackageTrackingDto(
                item.getNotifyNo(),
                info == null || info.getBidName() == null ? item.getFolderName() : info.getBidName(),
                info == null || info.getInvestor() == null ? null : info.getInvestor().getInvestorName(),
                item.getFolderName(),
                item.getSourcePath(),
                folderExists(item.getSourcePath()),
                info == null ? null : firstNonNull(info.getBidEstimatePrice(), info.getBidPrice()),
                formatDate(item.getCreatedAt()),
                bidding == null ? null : bidding.getBidCloseAt(),
                bidding == null ? null : bidding.getBidOpenAt(),
                executionTime(info, winner),
                winner == null ? null : winner.getWinningPrice(),
                winningContractor(winner),
                contract == null ? null : contract.getBidUrl(),
                lifecycle(contract),
                item.getSyncStatus() == null ? null : item.getSyncStatus().name(),
                item.getLastSyncedAt(),
                item.getLastError(),
                completenessPercent(info, bidding, bidderDtos, winner),
                downloadCompletenessPercent(documents),
                documents.size(),
                downloadedDocumentCount(documents),
                failedDocumentCount(documents),
                pendingDocumentCount(documents),
                apiCoverage(info, bidding, bidderDtos, winner, documents),
                bidderDtos,
                logs(item),
                missingFields(info, bidding),
                item.getLastError() == null ? 0 : 1
        );
    }

    private List<BidderDto> toBidderDtos(List<BiddingContractor> bidders) {
        List<BiddingContractor> sorted = bidders.stream()
                .sorted(Comparator.comparing(BiddingContractor::getId))
                .toList();
        List<BidderDto> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            BiddingContractor bidder = sorted.get(i);
            result.add(new BidderDto(
                    i + 1,
                    firstTaxCode(bidder),
                    bidder.getContractor() == null ? null : bidder.getContractor().getContractorName(),
                    bidder.getBidPrice(),
                    bidder.getDiscountRate(),
                    bidder.getBidPriceAfterDiscount(),
                    bidder.getBidValidityPeriod(),
                    bidder.getBidGuaranteeValue() == null ? null : bidder.getBidGuaranteeValue() + " VND",
                    bidder.getBidGuaranteeValidityPeriod(),
                    bidder.getContractExecutionTime()
            ));
        }
        return result;
    }

    private String firstTaxCode(BiddingContractor bidder) {
        if (bidder.getContractor() == null || bidder.getContractor().getTaxCodes() == null || bidder.getContractor().getTaxCodes().isEmpty()) {
            return null;
        }
        return bidder.getContractor().getTaxCodes().get(0);
    }

    private String executionTime(ContractInfo info, BiddingResult winner) {
        if (winner != null) {
            if (winner.getContractExecutionTime() != null && !winner.getContractExecutionTime().isBlank()) {
                return winner.getContractExecutionTime();
            }
            if (winner.getContractPeriodText() != null && !winner.getContractPeriodText().isBlank()) {
                return winner.getContractPeriodText();
            }
            if (winner.getContractPeriod() != null) {
                return winner.getContractPeriod() + " " + Optional.ofNullable(winner.getContractPeriodUnit()).orElse("ngay");
            }
        }
        if (info != null && info.getContractPeriod() != null) {
            return info.getContractPeriod() + " " + Optional.ofNullable(info.getContractPeriodUnit()).orElse("ngay");
        }
        return null;
    }

    private String winningContractor(BiddingResult winner) {
        if (winner == null || winner.getBiddingContractor() == null || winner.getBiddingContractor().getContractor() == null) {
            return null;
        }
        return winner.getBiddingContractor().getContractor().getContractorName();
    }

    private String lifecycle(Contract contract) {
        if (contract == null || contract.getBidStatus() == null) {
            return "Không xác định";
        }
        return switch (contract.getBidStatus()) {
            case INVITATION_OPEN -> "Đang mời thầu";
            case BID_OPENED -> "Đã mở thầu";
            case BIDDING_CLOSED -> "Đã đóng thầu";
            case CONTRACTOR_SELECTION_RESULT_AVAILABLE -> "Có KQLCNT";
            case CONTRACT_INFORMATION_AVAILABLE -> "Có thông tin hợp đồng";
        };
    }

    private Integer completenessPercent(ContractInfo info, Bidding bidding, List<BidderDto> bidders, BiddingResult winner) {
        int total = 6;
        int done = 0;
        if (info != null && info.getBidName() != null) done++;
        if (info != null && (info.getBidEstimatePrice() != null || info.getBidPrice() != null)) done++;
        if (info != null && info.getInvestor() != null) done++;
        if (bidding != null && bidding.getBidCloseAt() != null) done++;
        if (bidding != null && bidding.getBidOpenAt() != null) done++;
        if (!bidders.isEmpty() || winner != null) done++;
        return done * 100 / total;
    }

    private Integer downloadCompletenessPercent(List<BiddingDocument> documents) {
        if (documents.isEmpty()) {
            return 0;
        }
        int completed = downloadedDocumentCount(documents);
        return (int) (completed * 100 / documents.size());
    }

    private Integer downloadedDocumentCount(List<BiddingDocument> documents) {
        return (int) documents.stream()
                .filter(document -> "DOWNLOADED".equalsIgnoreCase(document.getDownloadStatus()) || document.getDownloadedAt() != null)
                .count();
    }

    private Integer failedDocumentCount(List<BiddingDocument> documents) {
        return (int) documents.stream()
                .filter(document -> "FAILED".equalsIgnoreCase(document.getDownloadStatus()))
                .count();
    }

    private Integer pendingDocumentCount(List<BiddingDocument> documents) {
        int pending = documents.size() - downloadedDocumentCount(documents) - failedDocumentCount(documents);
        return Math.max(pending, 0);
    }

    private Map<String, String> apiCoverage(ContractInfo info, Bidding bidding, List<BidderDto> bidders, BiddingResult winner, List<BiddingDocument> documents) {
        Map<String, String> coverage = new LinkedHashMap<>();
        coverage.put("Search", info == null ? "Missing" : "Success");
        coverage.put("TBMT", info == null ? "Missing" : "Success");
        coverage.put("HSMT", documents.isEmpty() ? "Missing" : "Success");
        coverage.put("BaoCaoOpenDetail", bidders.isEmpty() ? "Missing" : "Success");
        coverage.put("KQLCNT", winner == null ? "Missing" : "Success");
        coverage.put("Contract", bidding == null ? "Missing" : "Success");
        return coverage;
    }

    private List<ScrapingLogDto> logs(BidPackageSyncItem item) {
        if (item.getLastError() == null || item.getLastError().isBlank()) {
            return List.of();
        }
        return List.of(new ScrapingLogDto(
                formatDateTime(firstNonNull(item.getLastAttemptedAt(), item.getUpdatedAt())),
                "ERROR",
                "Sync",
                item.getLastError()
        ));
    }

    private List<MissingFieldDto> missingFields(ContractInfo info, Bidding bidding) {
        List<MissingFieldDto> missingFields = new ArrayList<>();
        if (info == null || info.getBidName() == null) {
            missingFields.add(new MissingFieldDto("title", "TBMT", "Chưa có tên gói thầu", "Chạy đồng bộ gói thầu"));
        }
        if (info == null || info.getInvestor() == null) {
            missingFields.add(new MissingFieldDto("investor", "TBMT", "Chưa có bên mời thầu", "Chạy đồng bộ gói thầu"));
        }
        if (bidding == null || bidding.getBidCloseAt() == null) {
            missingFields.add(new MissingFieldDto("closeTime", "TBMT", "Chưa có thời điểm đóng thầu", "Chạy đồng bộ TBMT"));
        }
        return missingFields;
    }

    private Boolean folderExists(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            return false;
        }
        try {
            return Files.isDirectory(Path.of(folderPath.trim()));
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim()) ? null : value.trim();
    }

    private String formatDate(OffsetDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }


    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateFolderPath(String notifyNo, String folderPath) {
        BidPackageSyncItem item = syncItemRepository.findByNotifyNo(notifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Sync item not found: " + notifyNo));
        item.setSourcePath(folderPath);
        syncItemRepository.save(item);
    }
}
