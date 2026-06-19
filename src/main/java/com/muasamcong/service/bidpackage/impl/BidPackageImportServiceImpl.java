package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.model.SyncItem;
import com.muasamcong.model.SyncSource;
import com.muasamcong.repository.SyncItemRepository;
import com.muasamcong.repository.SyncSourceRepository;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import com.muasamcong.service.storage.SyncFolderEntry;
import com.muasamcong.service.storage.SyncStorageService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidPackageImportServiceImpl implements BidPackageImportService {
    private static final Pattern NOTIFY_NO_PATTERN = Pattern.compile("\\bIB\\d{10}\\b", Pattern.CASE_INSENSITIVE);

    private final SyncItemRepository syncItemRepository;
    private final SyncSourceRepository syncSourceRepository;
    private final SyncStorageService syncStorageService;

    @Override
    @Transactional
    public BidPackageFolderImportResult importFolders(BidPackageFolderImportRequest request) {
        if (request == null || request.folderPaths() == null || request.folderPaths().isEmpty()) {
            throw new IllegalArgumentException("folderPaths is required");
        }

        Map<String, FolderCandidate> candidatesByNotifyNo = new LinkedHashMap<>();
        int totalFolders = 0;
        int invalid = 0;
        int failed = 0;

        for (String folderPath : request.folderPaths()) {
            if (folderPath == null || folderPath.isBlank()) {
                invalid++;
                continue;
            }

            String sourceParentPath = folderPath.trim();
            Optional<SyncSource> syncSource = syncSourceRepository.findByPath(sourceParentPath);

            try {
                List<SyncFolderEntry> childFolders = syncStorageService.listDirectories(sourceParentPath).stream()
                        .sorted(Comparator.comparing(entry -> naturalSortKey(entry.name()), String.CASE_INSENSITIVE_ORDER))
                        .toList();

                for (int i = 0; i < childFolders.size(); i++) {
                    SyncFolderEntry childFolder = childFolders.get(i);
                    totalFolders++;
                    String folderName = childFolder.name();
                    String notifyNo = extractNotifyNo(folderName);
                    if (notifyNo == null) {
                        invalid++;
                        continue;
                    }

                    candidatesByNotifyNo.putIfAbsent(notifyNo, new FolderCandidate(
                            notifyNo,
                            folderName,
                            childFolder.path(),
                            sourceParentPath,
                            syncSource.orElse(null),
                            i
                    ));
                }
            } catch (Exception ex) {
                failed++;
                log.warn("Import bid packages failed parentPath={}, error={}", sourceParentPath, ex.getMessage());
            }
        }

        Set<String> existingNotifyNos = candidatesByNotifyNo.isEmpty()
                ? Set.of()
                : syncItemRepository.findByNotifyNoIn(candidatesByNotifyNo.keySet())
                .stream()
                .map(SyncItem::getNotifyNo)
                .collect(Collectors.toSet());
        List<String> existedNotifyNos = candidatesByNotifyNo.keySet().stream()
                .filter(existingNotifyNos::contains)
                .toList();

        List<SyncItem> newItems = new ArrayList<>();
        for (FolderCandidate candidate : candidatesByNotifyNo.values()) {
            if (existingNotifyNos.contains(candidate.notifyNo())) {
                continue;
            }

            SyncItem item = new SyncItem();
            item.setNotifyNo(candidate.notifyNo());
            item.setFolderName(candidate.folderName());
            item.setSyncSource(candidate.syncSource());
            item.setSourcePath(candidate.sourcePath());
            item.setSourceParentPath(candidate.sourceParentPath());
            item.setSourceOrder(candidate.sourceOrder());
            item.setSyncStatus(BidPackageSyncStatus.PENDING);
            newItems.add(item);
        }

        syncItemRepository.saveAll(newItems);

        int created = newItems.size();
        int existed = candidatesByNotifyNo.size() - created;
        String message = existedNotifyNos.isEmpty()
                ? "Import completed"
                : "Skipped existing notifyNo: " + String.join(", ", existedNotifyNos);
        log.info("Import bid packages done totalFolders={}, created={}, existed={}, invalid={}, failed={}",
                totalFolders, created, existed, invalid, failed);

        return new BidPackageFolderImportResult(totalFolders, created, existed, invalid, failed, message, existedNotifyNos);
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

    private record FolderCandidate(
            String notifyNo,
            String folderName,
            String sourcePath,
            String sourceParentPath,
            SyncSource syncSource,
            Integer sourceOrder
    ) {
    }
}
