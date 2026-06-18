package com.muasamcong.service.bidpackage.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.dto.bidpackage.BidPackageSyncPendingResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemRunResult;
import com.muasamcong.dto.bidpackage.syncsystem.BidPackageSyncSystemUpdateRequest;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.BidPackageSyncRootFolder;
import com.muasamcong.model.BidPackageSyncSystem;
import com.muasamcong.repository.BidPackageSyncRootFolderRepository;
import com.muasamcong.repository.BidPackageSyncSystemRepository;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import com.muasamcong.service.bidpackage.BidPackageSyncQueueService;
import com.muasamcong.service.bidpackage.BidPackageSyncSystemService;
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
public class BidPackageSyncSystemServiceImpl implements BidPackageSyncSystemService {
    private static final int DEFAULT_INTERVAL_MINUTES = 30;
    private static final int DEFAULT_BATCH_LIMIT = 50;
    private static final int MIN_INTERVAL_MINUTES = 1;
    private static final int MAX_INTERVAL_MINUTES = 1440;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final BidPackageSyncSystemRepository repository;
    private final BidPackageSyncRootFolderRepository rootFolderRepository;
    private final BidPackageImportService importService;
    private final BidPackageSyncQueueService syncQueueService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public BidPackageSyncSystemResult getConfig() {
        return toResult(config());
    }

    @Override
    @Transactional
    public BidPackageSyncSystemResult updateConfig(BidPackageSyncSystemUpdateRequest request) {
        BidPackageSyncSystem config = config();
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        int intervalMinutes = normalizeInterval(request.intervalMinutes());
        int batchLimit = 0;
        List<String> importRootPaths = cleanRootPaths(request.importRootPaths());
        if (!importRootPaths.isEmpty()) {
            upsertRootFolders(importRootPaths);
        }
        if (enabled && activeRootFolders().isEmpty()) {
            throw new IllegalArgumentException("At least one active import root folder is required when sync system is enabled");
        }

        config.setEnabled(enabled);
        config.setIntervalMinutes(intervalMinutes);
        config.setBatchLimit(batchLimit);
        config.setNextRunAt(enabled ? OffsetDateTime.now().plusMinutes(intervalMinutes) : null);
        return toResult(repository.save(config));
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
        repository.findTopByOrderByIdAsc()
                .filter(config -> Boolean.TRUE.equals(config.getRunning()))
                .ifPresent(config -> {
                    config.setRunning(false);
                    config.setLastStatus(STATUS_FAILED);
                    config.setLastError("Previous sync run was interrupted by application restart");
                    config.setNextRunAt(Boolean.TRUE.equals(config.getEnabled())
                            ? OffsetDateTime.now().plusMinutes(config.getIntervalMinutes())
                            : null);
                    repository.save(config);
                    log.warn("Reset interrupted bid package sync system run");
                });
    }

    private BidPackageSyncSystemRunResult run(boolean scheduled) {
        BidPackageSyncSystem config = config();
        OffsetDateTime now = OffsetDateTime.now();
        if (scheduled && !Boolean.TRUE.equals(config.getEnabled())) {
            return skipped(config, "Sync system is disabled");
        }
        if (Boolean.TRUE.equals(config.getRunning())) {
            return skipped(config, "Sync system is already running");
        }
        if (scheduled && config.getNextRunAt() != null && now.isBefore(config.getNextRunAt())) {
            return skipped(config, "Next run time has not arrived");
        }

        migrateLegacyRootFolders(config);
        List<BidPackageSyncRootFolder> activeRootFolders = activeRootFolders();
        if (activeRootFolders.isEmpty()) {
            config.setLastStatus(STATUS_FAILED);
            config.setLastError("At least one active import root folder is required");
            repository.save(config);
            throw new IllegalStateException("At least one active import root folder is required");
        }

            config.setRunning(true);
            config.setLastError(null);
            config.setStartedAt(now);
            config.setEndedAt(null);
            resetProgress(config);
            repository.save(config);

        try {
            BidPackageFolderImportResult importResult = importActiveRootFolders(activeRootFolders);
            BidPackageSyncPendingResult syncPendingResult = syncQueueService.syncPending(0);
            BidPackageSyncPendingResult refreshSuccessResult = syncQueueService.refreshSuccess(0);

            OffsetDateTime finishedAt = OffsetDateTime.now();
            applyProgress(config, syncPendingResult, refreshSuccessResult);
            config.setEndedAt(finishedAt);
            config.setLastRunAt(finishedAt);
            config.setNextRunAt(Boolean.TRUE.equals(config.getEnabled()) ? finishedAt.plusMinutes(config.getIntervalMinutes()) : null);
            config.setLastStatus(STATUS_SUCCESS);
            config.setLastError(null);
            config.setCurrentNotifyNo(null);
            config.setRunning(false);
            repository.save(config);
            log.info("Bid package sync system run done importCreated={}, pendingSuccess={}, refreshSuccess={}",
                    importResult.created(), syncPendingResult.success(), refreshSuccessResult.success());

            return new BidPackageSyncSystemRunResult(
                    false,
                    "Sync system run completed",
                    importResult,
                    syncPendingResult,
                    refreshSuccessResult,
                    toResult(config)
            );
        } catch (Exception ex) {
            OffsetDateTime failedAt = OffsetDateTime.now();
            config.setEndedAt(failedAt);
            config.setLastRunAt(failedAt);
            config.setNextRunAt(Boolean.TRUE.equals(config.getEnabled()) ? failedAt.plusMinutes(config.getIntervalMinutes()) : null);
            config.setLastStatus(STATUS_FAILED);
            config.setLastError(safeMessage(ex));
            config.setCurrentNotifyNo(null);
            config.setRunning(false);
            repository.save(config);
            log.warn("Bid package sync system run failed error={}", safeMessage(ex));
            throw new IllegalStateException("Sync system run failed: " + safeMessage(ex), ex);
        }
    }

    private BidPackageSyncSystemRunResult skipped(BidPackageSyncSystem config, String message) {
        return new BidPackageSyncSystemRunResult(true, message, null, null, null, toResult(config));
    }

    private BidPackageSyncSystem config() {
        return repository.findTopByOrderByIdAsc().orElseGet(() -> {
            BidPackageSyncSystem config = new BidPackageSyncSystem();
            config.setEnabled(false);
                config.setIntervalMinutes(DEFAULT_INTERVAL_MINUTES);
                config.setBatchLimit(0);
                config.setImportRootPaths("[]");
            config.setRunning(false);
            return repository.save(config);
        });
    }

    private BidPackageSyncSystemResult toResult(BidPackageSyncSystem config) {
        migrateLegacyRootFolders(config);
        return new BidPackageSyncSystemResult(
                config.getEnabled(),
                config.getIntervalMinutes(),
                config.getBatchLimit(),
                activeRootFolders().stream().map(BidPackageSyncRootFolder::getPath).toList(),
                config.getLastRunAt(),
                config.getNextRunAt(),
                config.getRunning(),
                config.getLastStatus(),
                config.getLastError(),
                config.getStartedAt(),
                config.getEndedAt(),
                config.getTotalItems(),
                config.getProcessedItems(),
                config.getSuccessItems(),
                config.getFailedItems(),
                config.getSkippedItems(),
                config.getCurrentNotifyNo()
        );
    }

    private void resetProgress(BidPackageSyncSystem config) {
        config.setTotalItems(0);
        config.setProcessedItems(0);
        config.setSuccessItems(0);
        config.setFailedItems(0);
        config.setSkippedItems(0);
        config.setCurrentNotifyNo(null);
    }

    private void applyProgress(
            BidPackageSyncSystem config,
            BidPackageSyncPendingResult syncPendingResult,
            BidPackageSyncPendingResult refreshSuccessResult
    ) {
        int total = syncPendingResult.total() + refreshSuccessResult.total();
        int success = syncPendingResult.success() + refreshSuccessResult.success();
        int failed = syncPendingResult.failed() + refreshSuccessResult.failed();
        config.setTotalItems(total);
        config.setProcessedItems(success + failed);
        config.setSuccessItems(success);
        config.setFailedItems(failed);
        config.setSkippedItems(0);
    }

    private int normalizeInterval(Integer value) {
        int interval = value == null ? DEFAULT_INTERVAL_MINUTES : value;
        if (interval < MIN_INTERVAL_MINUTES || interval > MAX_INTERVAL_MINUTES) {
            throw new IllegalArgumentException("intervalMinutes must be between 1 and 1440");
        }
        return interval;
    }

    private int normalizeBatchLimit(Integer value) {
        return 0;
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

    private List<BidPackageSyncRootFolder> activeRootFolders() {
        return rootFolderRepository.findByStatusOrderByCreatedAtAsc(RecordStatus.ACTIVE);
    }

    private void upsertRootFolders(List<String> rootPaths) {
        cleanRootPaths(rootPaths).forEach(path -> {
            BidPackageSyncRootFolder folder = rootFolderRepository.findByPath(path).orElseGet(() -> {
                BidPackageSyncRootFolder newFolder = new BidPackageSyncRootFolder();
                newFolder.setPath(path);
                return newFolder;
            });
            folder.setStatus(RecordStatus.ACTIVE);
            rootFolderRepository.save(folder);
        });
    }

    private void migrateLegacyRootFolders(BidPackageSyncSystem config) {
        if (rootFolderRepository.existsByStatusIn(List.of(RecordStatus.ACTIVE, RecordStatus.INACTIVE))) {
            return;
        }
        List<String> legacyPaths = readRootPaths(config.getImportRootPaths());
        if (!legacyPaths.isEmpty()) {
            upsertRootFolders(legacyPaths);
        }
    }

    private BidPackageFolderImportResult importActiveRootFolders(List<BidPackageSyncRootFolder> rootFolders) {
        int totalFolders = 0;
        int created = 0;
        int existed = 0;
        int invalid = 0;
        int failed = 0;
        List<String> existedNotifyNos = new ArrayList<>();

        for (BidPackageSyncRootFolder rootFolder : rootFolders) {
            try {
                BidPackageFolderImportResult result = importService.importFolders(new BidPackageFolderImportRequest(List.of(rootFolder.getPath())));
                totalFolders += result.totalFolders();
                created += result.created();
                existed += result.existed();
                invalid += result.invalid();
                failed += result.failed();
                existedNotifyNos.addAll(result.existedNotifyNos());
                rootFolder.setLastImportedAt(OffsetDateTime.now());
                rootFolder.setLastStatus(result.failed() > 0 ? STATUS_FAILED : STATUS_SUCCESS);
                rootFolder.setLastError(result.failed() > 0 ? result.message() : null);
                rootFolderRepository.save(rootFolder);
            } catch (Exception ex) {
                failed++;
                rootFolder.setLastImportedAt(OffsetDateTime.now());
                rootFolder.setLastStatus(STATUS_FAILED);
                rootFolder.setLastError(safeMessage(ex));
                rootFolderRepository.save(rootFolder);
                log.warn("Import root folder failed path={}, error={}", rootFolder.getPath(), safeMessage(ex));
            }
        }

        String message = failed > 0 ? "Import completed with folder errors" : "Import completed";
        return new BidPackageFolderImportResult(totalFolders, created, existed, invalid, failed, message, existedNotifyNos);
    }

    private String writeRootPaths(List<String> rootPaths) {
        try {
            return objectMapper.writeValueAsString(rootPaths == null ? List.of() : rootPaths);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid importRootPaths", ex);
        }
    }

    private List<String> readRootPaths(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return cleanRootPaths(objectMapper.readValue(value, new TypeReference<List<String>>() {
            }));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid importRootPaths config", ex);
        }
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
