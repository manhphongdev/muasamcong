package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderCreateResult;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderDuplicateResult;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderResult;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.SyncItem;
import com.muasamcong.model.SyncSource;
import com.muasamcong.repository.SyncItemRepository;
import com.muasamcong.repository.SyncSourceRepository;
import com.muasamcong.service.bidpackage.SyncSourceService;
import com.muasamcong.service.storage.SyncFolderEntry;
import com.muasamcong.service.storage.SyncStorageService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncSourceServiceImpl implements SyncSourceService {
    private static final Pattern NOTIFY_NO_PATTERN = Pattern.compile("\\bIB\\d{10}\\b", Pattern.CASE_INSENSITIVE);
    private static final String DUPLICATE_MESSAGE = "Gói thầu này đã tồn tại, hệ thống sẽ không sync lại từ folder mới.";

    private final SyncSourceRepository syncSourceRepository;
    private final SyncItemRepository syncItemRepository;
    private final SyncStorageService syncStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<BidPackageSyncRootFolderResult> list() {
        return syncSourceRepository.findByStatusInOrderByCreatedAtAsc(List.of(RecordStatus.ACTIVE, RecordStatus.INACTIVE)).stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    @Transactional
    public BidPackageSyncRootFolderCreateResult create(List<String> paths) {
        List<String> cleanPaths = cleanPaths(paths);
        cleanPaths.forEach(path -> {
            SyncSource syncSource = syncSourceRepository.findByPath(path).orElseGet(() -> {
                SyncSource newSyncSource = new SyncSource();
                newSyncSource.setPath(path);
                return newSyncSource;
            });
            syncSource.setStatus(RecordStatus.ACTIVE);
            syncSourceRepository.save(syncSource);
        });
        return new BidPackageSyncRootFolderCreateResult(list(), duplicatePackages(cleanPaths));
    }

    @Override
    @Transactional
    public BidPackageSyncRootFolderResult activate(Long id) {
        SyncSource syncSource = syncSource(id);
        syncSource.setStatus(RecordStatus.ACTIVE);
        return toResult(syncSourceRepository.save(syncSource));
    }

    @Override
    @Transactional
    public BidPackageSyncRootFolderResult deactivate(Long id) {
        SyncSource syncSource = syncSource(id);
        syncSource.setStatus(RecordStatus.INACTIVE);
        return toResult(syncSourceRepository.save(syncSource));
    }

    private SyncSource syncSource(Long id) {
        return syncSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Root folder not found: " + id));
    }

    private List<String> cleanPaths(List<String> paths) {
        if (paths == null) {
            return List.of();
        }
        return paths.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<BidPackageSyncRootFolderDuplicateResult> duplicatePackages(List<String> parentPaths) {
        Map<String, BidPackageSyncRootFolderDuplicateResult> duplicates = new LinkedHashMap<>();
        for (String parentPathValue : parentPaths) {
            try {
                syncStorageService.listDirectories(parentPathValue).stream()
                        .sorted(Comparator.comparing(entry -> naturalSortKey(entry.name()), String.CASE_INSENSITIVE_ORDER))
                        .forEach(childFolder -> addDuplicateIfExists(duplicates, childFolder));
            } catch (Exception ex) {
                log.warn("Scan root folder duplicates failed path={}, error={}", parentPathValue, ex.getMessage());
            }
        }
        return List.copyOf(duplicates.values());
    }

    private void addDuplicateIfExists(
            Map<String, BidPackageSyncRootFolderDuplicateResult> duplicates,
            SyncFolderEntry childFolder
    ) {
        String folderName = childFolder.name();
        String notifyNo = extractNotifyNo(folderName);
        if (notifyNo == null || duplicates.containsKey(notifyNo)) {
            return;
        }

        syncItemRepository.findByNotifyNo(notifyNo).ifPresent(existing -> duplicates.put(notifyNo, toDuplicateResult(
                notifyNo,
                folderName,
                childFolder.path(),
                existing
        )));
    }

    private BidPackageSyncRootFolderDuplicateResult toDuplicateResult(
            String notifyNo,
            String folderName,
            String newPath,
            SyncItem existing
    ) {
        return new BidPackageSyncRootFolderDuplicateResult(
                notifyNo,
                folderName,
                newPath,
                existing.getSourcePath(),
                DUPLICATE_MESSAGE
        );
    }

    private String extractNotifyNo(String folderName) {
        Matcher matcher = NOTIFY_NO_PATTERN.matcher(folderName);
        return matcher.find() ? matcher.group().toUpperCase() : null;
    }

    private String naturalSortKey(String value) {
        StringBuilder key = new StringBuilder();
        Matcher matcher = Pattern.compile("\\d+|\\D+").matcher(value);
        while (matcher.find()) {
            String part = matcher.group();
            if (Character.isDigit(part.charAt(0))) {
                key.append(String.format("%06d", part.length())).append(part);
            } else {
                key.append(part);
            }
        }
        return key.toString();
    }

    private BidPackageSyncRootFolderResult toResult(SyncSource syncSource) {
        return new BidPackageSyncRootFolderResult(
                syncSource.getId(),
                syncSource.getPath(),
                syncSource.getStatus() == null ? null : syncSource.getStatus().name(),
                syncSource.getLastImportedAt(),
                syncSource.getLastStatus(),
                syncSource.getLastError(),
                syncSource.getCreatedAt(),
                syncSource.getUpdatedAt()
        );
    }
}
