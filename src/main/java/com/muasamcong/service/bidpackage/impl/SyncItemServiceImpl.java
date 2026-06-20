package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.BidPackageSyncPendingItemResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.BidPackageTrackingDto;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.model.SyncItem;
import com.muasamcong.repository.SyncItemRepository;
import com.muasamcong.service.bidpackage.SyncItemService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncItemServiceImpl implements SyncItemService {
    private final SyncItemRepository syncItemRepository;
    private final BidPackageSyncPipeline syncPipeline;
    private final BidPackageTrackingReader trackingReader;

    @Override
    public BidPackageSyncPendingResult syncPending() {
        Pageable pageable = Pageable.unpaged();
        List<SyncItem> items = syncItemRepository.findSyncQueue(
                List.of(BidPackageSyncStatus.PENDING, BidPackageSyncStatus.FAILED),
                pageable
        );

        log.info("Sync bid packages pending start items={}", items.size());
        BidPackageSyncPendingResult result = syncItems(items);
        log.info("Sync bid packages pending done success={}, failed={}", result.success(), result.failed());
        return result;
    }

    @Override
    public BidPackageSyncPendingResult refreshSuccess() {
        Pageable pageable = Pageable.unpaged();
        List<SyncItem> items = syncItemRepository.findRefreshQueue(
                BidPackageSyncStatus.SUCCESS,
                pageable
        );

        log.info("Refresh bid packages success start items={}", items.size());
        BidPackageSyncPendingResult result = syncItems(items);
        log.info("Refresh bid packages success done success={}, failed={}", result.success(), result.failed());
        return result;
    }

    @Override
    public BidPackageSyncPendingItemResult syncByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        SyncItem item = syncItemRepository.findByNotifyNo(normalizedNotifyNo).orElseGet(() -> {
            SyncItem newItem = new SyncItem();
            newItem.setNotifyNo(normalizedNotifyNo);
            newItem.setSyncStatus(BidPackageSyncStatus.PENDING);
            return syncItemRepository.save(newItem);
        });

        return syncPipeline.sync(item);
    }

    @Override
    public Page<BidPackageTrackingDto> searchTracking(String search, String status, String kpiFilter, Pageable pageable) {
        return trackingReader.searchTracking(search, status, kpiFilter, pageable);
    }

    @Override
    @Transactional
    public void updateFolderPath(String notifyNo, String folderPath) {
        SyncItem item = syncItemRepository.findByNotifyNo(notifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Sync item not found: " + notifyNo));
        item.setSourcePath(folderPath);
        syncItemRepository.save(item);
    }

    private BidPackageSyncPendingResult syncItems(List<SyncItem> items) {
        List<BidPackageSyncPendingItemResult> results = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (SyncItem item : items) {
            BidPackageSyncPendingItemResult result = syncPipeline.sync(item);
            results.add(result);
            if (result.success()) {
                success++;
            } else {
                failed++;
            }
        }

        return new BidPackageSyncPendingResult(items.size(), success, failed, results);
    }

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim();
    }
}
