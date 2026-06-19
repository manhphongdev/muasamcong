package com.muasamcong.service.export.impl;

import com.muasamcong.dto.export.AutoDownloadExportItemResult;
import com.muasamcong.dto.export.AutoDownloadExportResult;
import com.muasamcong.service.bidopening.BidOpeningPdfService;
import com.muasamcong.service.biddingresult.BiddingResultGoodsExcelService;
import com.muasamcong.service.export.AutoDownloadExportService;
import com.muasamcong.service.storage.StoredFile;
import com.muasamcong.service.storage.SyncStorageService;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDownloadExportServiceImpl implements AutoDownloadExportService {
    private static final String TYPE_BID_OPENING = "BID_OPENING_PDF";
    private static final String TYPE_GOODS_EXCEL = "GOODS_EXCEL";
    private static final String BID_OPENING_FILE_NAME = "_Biên bản mở thầu.pdf";
    private static final String GOODS_FILE_PREFIX = "BẢNG DỰ THẦU HÀNG HÓA -";

    private final BidOpeningPdfService bidOpeningPdfService;
    private final BiddingResultGoodsExcelService biddingResultGoodsExcelService;
    private final SyncStorageService syncStorageService;

    @Override
    public AutoDownloadExportResult exportGeneratedFiles(String notifyNo, String sourcePath) {
        if (!StringUtils.hasText(sourcePath)) {
            return new AutoDownloadExportResult(2, 0, 0, 2, List.of(
                    skipped(TYPE_BID_OPENING, "Missing sourcePath"),
                    skipped(TYPE_GOODS_EXCEL, "Missing sourcePath")
            ));
        }

        String normalizedNotifyNo = notifyNo == null ? "" : notifyNo.trim();
        String autoDownloadPath = syncStorageService.resolveAutoDownloadPath(sourcePath);
        List<AutoDownloadExportItemResult> items = new ArrayList<>();
        items.add(exportBidOpening(normalizedNotifyNo, autoDownloadPath));
        items.add(exportGoods(normalizedNotifyNo, autoDownloadPath));

        int success = (int) items.stream().filter(AutoDownloadExportItemResult::success).count();
        int failed = (int) items.stream()
                .filter(item -> !item.success() && item.message() != null && !item.message().startsWith("Skipped"))
                .count();
        int skipped = items.size() - success - failed;
        return new AutoDownloadExportResult(items.size(), success, failed, skipped, items);
    }

    private AutoDownloadExportItemResult exportBidOpening(String notifyNo, String autoDownloadPath) {
        try {
            byte[] data = bidOpeningPdfService.renderByNotifyNo(notifyNo);
            StoredFile storedFile = write(autoDownloadPath, BID_OPENING_FILE_NAME, data);
            return success(TYPE_BID_OPENING, storedFile);
        } catch (Exception ex) {
            log.warn("Export bid opening PDF failed notifyNo={}, error={}", notifyNo, safeMessage(ex));
            return failed(TYPE_BID_OPENING, safeMessage(ex));
        }
    }

    private AutoDownloadExportItemResult exportGoods(String notifyNo, String autoDownloadPath) {
        try {
            byte[] data = biddingResultGoodsExcelService.exportByNotifyNo(notifyNo);
            StoredFile storedFile = write(autoDownloadPath, GOODS_FILE_PREFIX + notifyNo + ".xlsx", data);
            return success(TYPE_GOODS_EXCEL, storedFile);
        } catch (Exception ex) {
            log.warn("Export goods Excel failed notifyNo={}, error={}", notifyNo, safeMessage(ex));
            return failed(TYPE_GOODS_EXCEL, safeMessage(ex));
        }
    }

    private StoredFile write(String autoDownloadPath, String fileName, byte[] data) {
        return syncStorageService.write(
                autoDownloadPath,
                "",
                fileName,
                new ByteArrayInputStream(data)
        );
    }

    private AutoDownloadExportItemResult success(String type, StoredFile storedFile) {
        return new AutoDownloadExportItemResult(type, true, storedFile.path(), storedFile.size(), "Exported");
    }

    private AutoDownloadExportItemResult failed(String type, String message) {
        return new AutoDownloadExportItemResult(type, false, null, null, message);
    }

    private AutoDownloadExportItemResult skipped(String type, String message) {
        return new AutoDownloadExportItemResult(type, false, null, null, "Skipped: " + message);
    }

    private String safeMessage(Exception ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
