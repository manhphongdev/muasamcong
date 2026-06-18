package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderCreateResult;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderDuplicateResult;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderResult;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.BidPackageSyncItem;
import com.muasamcong.model.BidPackageSyncRootFolder;
import com.muasamcong.repository.BidPackageSyncItemRepository;
import com.muasamcong.repository.BidPackageSyncRootFolderRepository;
import com.muasamcong.service.bidpackage.BidPackageSyncRootFolderService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidPackageSyncRootFolderServiceImpl implements BidPackageSyncRootFolderService {
    private static final Pattern NOTIFY_NO_PATTERN = Pattern.compile("\\bIB\\d{10}\\b", Pattern.CASE_INSENSITIVE);
    private static final String DUPLICATE_MESSAGE = "Gói thầu này đã tồn tại, hệ thống sẽ không sync lại từ folder mới.";

    private final BidPackageSyncRootFolderRepository repository;
    private final BidPackageSyncItemRepository syncItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BidPackageSyncRootFolderResult> list() {
        return repository.findByStatusInOrderByCreatedAtAsc(List.of(RecordStatus.ACTIVE, RecordStatus.INACTIVE)).stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    @Transactional
    public BidPackageSyncRootFolderCreateResult create(List<String> paths) {
        List<String> cleanPaths = cleanPaths(paths);
        cleanPaths.forEach(path -> {
            BidPackageSyncRootFolder folder = repository.findByPath(path).orElseGet(() -> {
                BidPackageSyncRootFolder newFolder = new BidPackageSyncRootFolder();
                newFolder.setPath(path);
                return newFolder;
            });
            folder.setStatus(RecordStatus.ACTIVE);
            repository.save(folder);
        });
        return new BidPackageSyncRootFolderCreateResult(list(), duplicatePackages(cleanPaths));
    }

    @Override
    @Transactional
    public BidPackageSyncRootFolderResult activate(Long id) {
        BidPackageSyncRootFolder folder = folder(id);
        folder.setStatus(RecordStatus.ACTIVE);
        return toResult(repository.save(folder));
    }

    @Override
    @Transactional
    public BidPackageSyncRootFolderResult deactivate(Long id) {
        BidPackageSyncRootFolder folder = folder(id);
        folder.setStatus(RecordStatus.INACTIVE);
        return toResult(repository.save(folder));
    }

    private BidPackageSyncRootFolder folder(Long id) {
        return repository.findById(id)
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
            Path parentPath = Path.of(parentPathValue).toAbsolutePath().normalize();
            if (!Files.isDirectory(parentPath)) {
                continue;
            }

            try (Stream<Path> children = Files.list(parentPath)) {
                children
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(path -> naturalSortKey(path.getFileName().toString()), String.CASE_INSENSITIVE_ORDER))
                        .forEach(childFolder -> addDuplicateIfExists(duplicates, childFolder));
            } catch (IOException ex) {
                log.warn("Scan root folder duplicates failed path={}, error={}", parentPath, ex.getMessage());
            }
        }
        return List.copyOf(duplicates.values());
    }

    private void addDuplicateIfExists(
            Map<String, BidPackageSyncRootFolderDuplicateResult> duplicates,
            Path childFolder
    ) {
        String folderName = childFolder.getFileName().toString();
        String notifyNo = extractNotifyNo(folderName);
        if (notifyNo == null || duplicates.containsKey(notifyNo)) {
            return;
        }

        syncItemRepository.findByNotifyNo(notifyNo).ifPresent(existing -> duplicates.put(notifyNo, toDuplicateResult(
                notifyNo,
                folderName,
                childFolder.toAbsolutePath().normalize().toString(),
                existing
        )));
    }

    private BidPackageSyncRootFolderDuplicateResult toDuplicateResult(
            String notifyNo,
            String folderName,
            String newPath,
            BidPackageSyncItem existing
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

    private BidPackageSyncRootFolderResult toResult(BidPackageSyncRootFolder folder) {
        return new BidPackageSyncRootFolderResult(
                folder.getId(),
                folder.getPath(),
                folder.getStatus() == null ? null : folder.getStatus().name(),
                folder.getLastImportedAt(),
                folder.getLastStatus(),
                folder.getLastError(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }
}
