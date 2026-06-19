package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemRunResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemUpdateRequest;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.SyncJob;
import com.muasamcong.model.SyncSource;
import com.muasamcong.repository.SyncJobRepository;
import com.muasamcong.repository.SyncSourceRepository;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import com.muasamcong.service.bidpackage.SyncItemService;
import com.muasamcong.service.bidpackage.SyncJobService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncJobServiceImpl implements SyncJobService {
    private static final int DEFAULT_INTERVAL_MINUTES = 30;
    private static final int MIN_INTERVAL_MINUTES = 1;
    private static final int MAX_INTERVAL_MINUTES = 1440;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final SyncJobRepository syncJobRepository;
    private final SyncSourceRepository syncSourceRepository;
    private final BidPackageImportService importService;
    private final SyncItemService syncItemService;

    @Override
    @Transactional
    public BidPackageSyncSystemResult getConfig() {
        return toResult(syncJob());
    }

    @Override
    @Transactional
    public BidPackageSyncSystemResult updateConfig(BidPackageSyncSystemUpdateRequest request) {
        SyncJob syncJob = syncJob();
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        int intervalMinutes = normalizeInterval(request.intervalMinutes());
        List<String> importRootPaths = cleanRootPaths(request.importRootPaths());
        if (!importRootPaths.isEmpty()) {
            upsertRootFolders(importRootPaths);
        }
        if (enabled && activeSyncSources().isEmpty()) {
            throw new IllegalArgumentException("At least one active import root folder is required when sync system is enabled");
        }

        syncJob.setEnabled(enabled);
        syncJob.setIntervalMinutes(intervalMinutes);
        syncJob.setNextRunAt(enabled ? OffsetDateTime.now().plusMinutes(intervalMinutes) : null);
        return toResult(syncJobRepository.save(syncJob));
    }

    @Override
    public synchronized BidPackageSyncSystemRunResult runNow() {
        return run(false);
    }

    @Override
    public synchronized BidPackageSyncSystemRunResult runScheduledIfDue() {
        return run(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resetInterruptedRun() {
        syncJobRepository.findTopByOrderByIdAsc()
                .filter(syncJob -> Boolean.TRUE.equals(syncJob.getRunning()))
                .ifPresent(syncJob -> {
                    syncJob.setRunning(false);
                    syncJob.setLastStatus(STATUS_FAILED);
                    syncJob.setLastError("Previous sync run was interrupted by application restart");
                    syncJob.setNextRunAt(Boolean.TRUE.equals(syncJob.getEnabled())
                            ? OffsetDateTime.now().plusMinutes(syncJob.getIntervalMinutes())
                            : null);
                    syncJobRepository.save(syncJob);
                    log.warn("Reset interrupted bid package sync system run");
                });
    }

    private BidPackageSyncSystemRunResult run(boolean scheduled) {
        SyncJob syncJob = syncJob();
        OffsetDateTime now = OffsetDateTime.now();
        if (scheduled && !Boolean.TRUE.equals(syncJob.getEnabled())) {
            return skipped(syncJob, "Sync system is disabled");
        }
        if (Boolean.TRUE.equals(syncJob.getRunning())) {
            return skipped(syncJob, "Sync system is already running");
        }
        if (scheduled && syncJob.getNextRunAt() != null && now.isBefore(syncJob.getNextRunAt())) {
            return skipped(syncJob, "Next run time has not arrived");
        }

        List<SyncSource> activeSyncSources = activeSyncSources();
        if (activeSyncSources.isEmpty()) {
            syncJob.setLastStatus(STATUS_FAILED);
            syncJob.setLastError("At least one active import root folder is required");
            syncJobRepository.save(syncJob);
            throw new IllegalStateException("At least one active import root folder is required");
        }

            syncJob.setRunning(true);
            syncJob.setLastError(null);
            syncJob.setStartedAt(now);
            syncJob.setEndedAt(null);
            resetProgress(syncJob);
            syncJobRepository.save(syncJob);

        try {
            BidPackageFolderImportResult importResult = importActiveSyncSources(activeSyncSources);
            BidPackageSyncPendingResult syncPendingResult = syncItemService.syncPending(0);
            BidPackageSyncPendingResult refreshSuccessResult = syncItemService.refreshSuccess(0);

            OffsetDateTime finishedAt = OffsetDateTime.now();
            applyProgress(syncJob, syncPendingResult, refreshSuccessResult);
            syncJob.setEndedAt(finishedAt);
            syncJob.setLastRunAt(finishedAt);
            syncJob.setNextRunAt(Boolean.TRUE.equals(syncJob.getEnabled()) ? finishedAt.plusMinutes(syncJob.getIntervalMinutes()) : null);
            syncJob.setLastStatus(STATUS_SUCCESS);
            syncJob.setLastError(null);
            syncJob.setRunning(false);
            syncJobRepository.save(syncJob);
            log.info("Bid package sync system run done importCreated={}, pendingSuccess={}, refreshSuccess={}",
                    importResult.created(), syncPendingResult.success(), refreshSuccessResult.success());

            return new BidPackageSyncSystemRunResult(
                    false,
                    "Sync system run completed",
                    importResult,
                    syncPendingResult,
                    refreshSuccessResult,
                    toResult(syncJob)
            );
        } catch (Exception ex) {
            OffsetDateTime failedAt = OffsetDateTime.now();
            syncJob.setEndedAt(failedAt);
            syncJob.setLastRunAt(failedAt);
            syncJob.setNextRunAt(Boolean.TRUE.equals(syncJob.getEnabled()) ? failedAt.plusMinutes(syncJob.getIntervalMinutes()) : null);
            syncJob.setLastStatus(STATUS_FAILED);
            syncJob.setLastError(safeMessage(ex));
            syncJob.setRunning(false);
            syncJobRepository.save(syncJob);
            log.warn("Bid package sync system run failed error={}", safeMessage(ex));
            throw new IllegalStateException("Sync system run failed: " + safeMessage(ex), ex);
        }
    }

    private BidPackageSyncSystemRunResult skipped(SyncJob syncJob, String message) {
        return new BidPackageSyncSystemRunResult(true, message, null, null, null, toResult(syncJob));
    }

    private SyncJob syncJob() {
        return syncJobRepository.findTopByOrderByIdAsc().orElseGet(() -> {
            SyncJob syncJob = new SyncJob();
            syncJob.setEnabled(false);
                syncJob.setIntervalMinutes(DEFAULT_INTERVAL_MINUTES);
            syncJob.setRunning(false);
            return syncJobRepository.save(syncJob);
        });
    }

    private BidPackageSyncSystemResult toResult(SyncJob syncJob) {
        return new BidPackageSyncSystemResult(
                syncJob.getEnabled(),
                syncJob.getIntervalMinutes(),
                activeSyncSources().stream().map(SyncSource::getPath).toList(),
                syncJob.getLastRunAt(),
                syncJob.getNextRunAt(),
                syncJob.getRunning(),
                syncJob.getLastStatus(),
                syncJob.getLastError(),
                syncJob.getStartedAt(),
                syncJob.getEndedAt(),
                syncJob.getTotalItems(),
                syncJob.getSuccessItems(),
                syncJob.getFailedItems()
        );
    }

    private void resetProgress(SyncJob syncJob) {
        syncJob.setTotalItems(0);
        syncJob.setSuccessItems(0);
        syncJob.setFailedItems(0);
    }

    private void applyProgress(
            SyncJob syncJob,
            BidPackageSyncPendingResult syncPendingResult,
            BidPackageSyncPendingResult refreshSuccessResult
    ) {
        int total = syncPendingResult.total() + refreshSuccessResult.total();
        int success = syncPendingResult.success() + refreshSuccessResult.success();
        int failed = syncPendingResult.failed() + refreshSuccessResult.failed();
        syncJob.setTotalItems(total);
        syncJob.setSuccessItems(success);
        syncJob.setFailedItems(failed);
    }

    private int normalizeInterval(Integer value) {
        int interval = value == null ? DEFAULT_INTERVAL_MINUTES : value;
        if (interval < MIN_INTERVAL_MINUTES || interval > MAX_INTERVAL_MINUTES) {
            throw new IllegalArgumentException("intervalMinutes must be between 1 and 1440");
        }
        return interval;
    }

    private List<String> cleanRootPaths(List<String> rootPaths) {
        if (rootPaths == null) {
            return List.of();
        }
        return rootPaths.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<SyncSource> activeSyncSources() {
        return syncSourceRepository.findByStatusOrderByCreatedAtAsc(RecordStatus.ACTIVE);
    }

    private void upsertRootFolders(List<String> rootPaths) {
        cleanRootPaths(rootPaths).forEach(path -> {
            SyncSource syncSource = syncSourceRepository.findByPath(path).orElseGet(() -> {
                SyncSource newSyncSource = new SyncSource();
                newSyncSource.setPath(path);
                return newSyncSource;
            });
            syncSource.setStatus(RecordStatus.ACTIVE);
            syncSourceRepository.save(syncSource);
        });
    }

    private BidPackageFolderImportResult importActiveSyncSources(List<SyncSource> syncSources) {
        int totalFolders = 0;
        int created = 0;
        int existed = 0;
        int invalid = 0;
        int failed = 0;
        List<String> existedNotifyNos = new ArrayList<>();

        for (SyncSource syncSource : syncSources) {
            try {
                BidPackageFolderImportResult result = importService.importFolders(new BidPackageFolderImportRequest(List.of(syncSource.getPath())));
                totalFolders += result.totalFolders();
                created += result.created();
                existed += result.existed();
                invalid += result.invalid();
                failed += result.failed();
                existedNotifyNos.addAll(result.existedNotifyNos());
                syncSource.setLastImportedAt(OffsetDateTime.now());
                syncSource.setLastStatus(result.failed() > 0 ? STATUS_FAILED : STATUS_SUCCESS);
                syncSource.setLastError(result.failed() > 0 ? result.message() : null);
                syncSourceRepository.save(syncSource);
            } catch (Exception ex) {
                failed++;
                syncSource.setLastImportedAt(OffsetDateTime.now());
                syncSource.setLastStatus(STATUS_FAILED);
                syncSource.setLastError(safeMessage(ex));
                syncSourceRepository.save(syncSource);
                log.warn("Import root folder failed path={}, error={}", syncSource.getPath(), safeMessage(ex));
            }
        }

        String message = failed > 0 ? "Import completed with folder errors" : "Import completed";
        return new BidPackageFolderImportResult(totalFolders, created, existed, invalid, failed, message, existedNotifyNos);
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
