package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.integration.portal.PortalSearchClient;
import com.muasamcong.model.BidPackageSyncItem;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ProcurementPlan;
import com.muasamcong.repository.BidPackageSyncItemRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.ProcurementPlanRepository;
import com.muasamcong.service.bidopening.BidOpeningSyncService;
import com.muasamcong.service.bidpackage.BidPackageSyncQueueService;
import com.muasamcong.service.ingest.TbmtSyncService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidPackageSyncQueueServiceImpl implements BidPackageSyncQueueService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final BidPackageSyncItemRepository syncItemRepository;
    private final ProcurementPlanRepository procurementPlanRepository;
    private final ContractRepository contractRepository;
    private final PortalSearchClient portalSearchClient;
    private final TbmtSyncService tbmtSyncService;
    private final BidOpeningSyncService bidOpeningSyncService;

    @Override
    public BidPackageSyncPendingResult syncPending(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<BidPackageSyncItem> items = syncItemRepository.findBySyncStatusInOrderByCreatedAtAsc(
                List.of(BidPackageSyncStatus.PENDING, BidPackageSyncStatus.FAILED),
                PageRequest.of(0, safeLimit)
        );

        log.info("Sync bid packages pending start limit={}, items={}", safeLimit, items.size());
        BidPackageSyncPendingResult result = syncItems(items);
        log.info("Sync bid packages pending done success={}, failed={}", result.success(), result.failed());
        return result;
    }

    @Override
    public BidPackageSyncPendingResult refreshSuccess(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<BidPackageSyncItem> items = syncItemRepository.findBySyncStatusOrderByLastSyncedAtAsc(
                BidPackageSyncStatus.SUCCESS,
                PageRequest.of(0, safeLimit)
        );

        log.info("Refresh bid packages success start limit={}, items={}", safeLimit, items.size());
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
            Contract contract = contractRepository.findByNotifyNo(notifyNo)
                    .orElseGet(() -> createContract(notifyNo, procurementPlan));

            TbmtIngestResult ingestResult = tbmtSyncService.syncByNotifyNo(notifyNo);
            bidOpeningSyncService.syncByNotifyNo(notifyNo);

            item.setSyncStatus(BidPackageSyncStatus.SUCCESS);
            item.setLastSyncedAt(OffsetDateTime.now());
            item.setLastError(null);
            syncItemRepository.save(item);

            return new BidPackageSyncPendingItemResult(
                    notifyNo,
                    true,
                    "Synced",
                    item.getId(),
                    contract.getId(),
                    ingestResult.contractInfoId()
            );
        } catch (Exception ex) {
            item.setSyncStatus(BidPackageSyncStatus.FAILED);
            item.setRetryCount(item.getRetryCount() + 1);
            item.setLastError(ex.getMessage());
            syncItemRepository.save(item);
            log.warn("Sync bid package failed notifyNo={}, error={}", notifyNo, ex.getMessage());

            return new BidPackageSyncPendingItemResult(
                    notifyNo,
                    false,
                    ex.getMessage(),
                    item.getId(),
                    null,
                    null
            );
        }
    }

    private ProcurementPlan createProcurementPlan(String planNo) {
        ProcurementPlan procurementPlan = new ProcurementPlan();
        procurementPlan.setPlanNo(planNo);
        procurementPlan.setFetchedAt(OffsetDateTime.now());
        return procurementPlanRepository.save(procurementPlan);
    }

    private Contract createContract(String notifyNo, ProcurementPlan procurementPlan) {
        Contract contract = new Contract();
        contract.setNotifyNo(notifyNo);
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
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
