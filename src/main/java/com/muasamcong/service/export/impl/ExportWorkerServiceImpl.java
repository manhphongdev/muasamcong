package com.muasamcong.service.export.impl;

import com.muasamcong.dto.export.AutoDownloadExportItemResult;
import com.muasamcong.dto.export.AutoDownloadExportResult;
import com.muasamcong.enums.BidPackageSyncStatus;
import com.muasamcong.model.SyncItem;
import com.muasamcong.repository.SyncItemRepository;
import com.muasamcong.service.export.AutoDownloadExportService;
import com.muasamcong.service.export.ExportWorkerService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportWorkerServiceImpl implements ExportWorkerService {
    private final SyncItemRepository syncItemRepository;
    private final AutoDownloadExportService autoDownloadExportService;

    @Override
    public AutoDownloadExportResult exportSuccessfulPackages() {
        List<SyncItem> items = syncItemRepository.findRefreshQueue(BidPackageSyncStatus.SUCCESS, Pageable.unpaged());
        log.info("Export worker start items={}", items.size());

        List<AutoDownloadExportItemResult> exportItems = new ArrayList<>();
        int total = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;

        for (SyncItem item : items) {
            AutoDownloadExportResult result = autoDownloadExportService.exportGeneratedFiles(item.getNotifyNo(), item.getSourcePath());
            total += result.total();
            success += result.success();
            failed += result.failed();
            skipped += result.skipped();
            exportItems.addAll(result.items());
        }

        log.info("Export worker done total={}, success={}, failed={}, skipped={}", total, success, failed, skipped);
        return new AutoDownloadExportResult(total, success, failed, skipped, exportItems);
    }
}
