package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.model.BidPackageSyncItem;
import com.muasamcong.repository.BidPackageSyncItemRepository;
import com.muasamcong.service.bidpackage.BidPackageImportService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidPackageImportServiceImpl implements BidPackageImportService {
    private static final Pattern NOTIFY_NO_PATTERN = Pattern.compile("\\bIB\\d{10}\\b", Pattern.CASE_INSENSITIVE);

    private final BidPackageSyncItemRepository repository;

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

            Path parentPath = Path.of(folderPath.trim()).toAbsolutePath().normalize();
            if (!Files.isDirectory(parentPath)) {
                failed++;
                log.warn("Import bid packages skipped invalid parentPath={}", parentPath);
                continue;
            }

            try (Stream<Path> children = Files.list(parentPath)) {
                List<Path> childFolders = children
                        .filter(Files::isDirectory)
                        .toList();

                for (Path childFolder : childFolders) {
                    totalFolders++;
                    String folderName = childFolder.getFileName().toString();
                    String notifyNo = extractNotifyNo(folderName);
                    if (notifyNo == null) {
                        invalid++;
                        continue;
                    }

                    candidatesByNotifyNo.putIfAbsent(notifyNo, new FolderCandidate(
                            notifyNo,
                            folderName,
                            childFolder.toAbsolutePath().normalize().toString(),
                            parentPath.getFileName().toString(),
                            parentPath.toString()
                    ));
                }
            } catch (IOException ex) {
                failed++;
                log.warn("Import bid packages failed parentPath={}, error={}", parentPath, ex.getMessage());
            }
        }

        Set<String> existingNotifyNos = candidatesByNotifyNo.isEmpty()
                ? Set.of()
                : repository.findByNotifyNoIn(candidatesByNotifyNo.keySet())
                .stream()
                .map(BidPackageSyncItem::getNotifyNo)
                .collect(Collectors.toSet());

        List<BidPackageSyncItem> newItems = new ArrayList<>();
        for (FolderCandidate candidate : candidatesByNotifyNo.values()) {
            if (existingNotifyNos.contains(candidate.notifyNo())) {
                continue;
            }

            BidPackageSyncItem item = new BidPackageSyncItem();
            item.setNotifyNo(candidate.notifyNo());
            item.setFolderName(candidate.folderName());
            item.setSourcePath(candidate.sourcePath());
            item.setParentFolderName(candidate.parentFolderName());
            item.setParentPath(candidate.parentPath());
            item.setSyncStatus(BidPackageSyncStatus.PENDING);
            newItems.add(item);
        }

        repository.saveAll(newItems);

        int created = newItems.size();
        int existed = candidatesByNotifyNo.size() - created;
        log.info("Import bid packages done totalFolders={}, created={}, existed={}, invalid={}, failed={}",
                totalFolders, created, existed, invalid, failed);

        return new BidPackageFolderImportResult(totalFolders, created, existed, invalid, failed);
    }

    private String extractNotifyNo(String folderName) {
        Matcher matcher = NOTIFY_NO_PATTERN.matcher(folderName);
        return matcher.find() ? matcher.group().toUpperCase() : null;
    }

    private record FolderCandidate(
            String notifyNo,
            String folderName,
            String sourcePath,
            String parentFolderName,
            String parentPath
    ) {
    }
}
