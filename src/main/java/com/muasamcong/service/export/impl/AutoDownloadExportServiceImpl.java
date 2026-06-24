package com.muasamcong.service.export.impl;

import com.muasamcong.dto.export.AutoDownloadExportItemResult;
import com.muasamcong.dto.export.AutoDownloadExportResult;
import com.muasamcong.model.BiddingDocument;
import com.muasamcong.model.Contract;
import com.muasamcong.repository.BiddingDocumentRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.service.bidopening.BidOpeningPdfService;
import com.muasamcong.service.biddingresult.BiddingResultGoodsExcelService;
import com.muasamcong.service.export.AutoDownloadExportService;
import com.muasamcong.service.storage.StoredFile;
import com.muasamcong.service.storage.SyncStorageService;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDownloadExportServiceImpl implements AutoDownloadExportService {
    private static final String TYPE_BID_OPENING = "BID_OPENING_PDF";
    private static final String TYPE_GOODS_EXCEL = "GOODS_EXCEL";
    private static final String GENERATED_BID_OPENING_ID = "GENERATED_BID_OPENING_PDF";
    private static final String GENERATED_GOODS_EXCEL_ID = "GENERATED_GOODS_EXCEL";
    private static final String SOURCE_GENERATED_EXPORT = "GENERATED_EXPORT";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String BID_OPENING_FILE_NAME = "_Biên bản mở thầu.pdf";
    private static final String GOODS_FILE_PREFIX = "BẢNG DỰ THẦU HÀNG HÓA -";

    private final BidOpeningPdfService bidOpeningPdfService;
    private final BiddingResultGoodsExcelService biddingResultGoodsExcelService;
    private final SyncStorageService syncStorageService;
    private final ContractRepository contractRepository;
    private final BiddingDocumentRepository biddingDocumentRepository;

    @Override
    @Transactional
    public AutoDownloadExportResult exportGeneratedFiles(String notifyNo, String sourcePath) {
        if (!StringUtils.hasText(sourcePath)) {
            return new AutoDownloadExportResult(2, 0, 0, 2, List.of(
                    skipped(TYPE_BID_OPENING, "Missing sourcePath"),
                    skipped(TYPE_GOODS_EXCEL, "Missing sourcePath")
            ));
        }

        String normalizedNotifyNo = notifyNo == null ? "" : notifyNo.trim();
        String autoDownloadPath = syncStorageService.resolveAutoDownloadPath(sourcePath);
        Optional<Contract> contract = StringUtils.hasText(normalizedNotifyNo)
                ? contractRepository.findByNotifyNo(normalizedNotifyNo)
                : Optional.empty();
        List<AutoDownloadExportItemResult> items = new ArrayList<>();
        items.add(exportBidOpening(normalizedNotifyNo, autoDownloadPath, contract.orElse(null)));
        items.add(exportGoods(normalizedNotifyNo, autoDownloadPath, contract.orElse(null)));

        int success = (int) items.stream().filter(AutoDownloadExportItemResult::success).count();
        int failed = (int) items.stream()
                .filter(item -> !item.success() && item.message() != null && !item.message().startsWith("Skipped"))
                .count();
        int skipped = items.size() - success - failed;
        return new AutoDownloadExportResult(items.size(), success, failed, skipped, items);
    }

    private AutoDownloadExportItemResult exportBidOpening(String notifyNo, String autoDownloadPath, Contract contract) {
        try {
            byte[] data = bidOpeningPdfService.renderByNotifyNo(notifyNo);
            StoredFile storedFile = write(autoDownloadPath, BID_OPENING_FILE_NAME, data);
            saveGeneratedDocument(contract, GENERATED_BID_OPENING_ID, BID_OPENING_FILE_NAME, TYPE_BID_OPENING, storedFile, null);
            return success(TYPE_BID_OPENING, storedFile);
        } catch (Exception ex) {
            log.warn("Export bid opening PDF failed notifyNo={}, error={}", notifyNo, safeMessage(ex));
            saveGeneratedDocument(contract, GENERATED_BID_OPENING_ID, BID_OPENING_FILE_NAME, TYPE_BID_OPENING, null, safeMessage(ex));
            return failed(TYPE_BID_OPENING, safeMessage(ex));
        }
    }

    private AutoDownloadExportItemResult exportGoods(String notifyNo, String autoDownloadPath, Contract contract) {
        String fileName = GOODS_FILE_PREFIX + notifyNo + ".xlsx";
        try {
            byte[] data = biddingResultGoodsExcelService.exportByNotifyNo(notifyNo);
            StoredFile storedFile = write(autoDownloadPath, fileName, data);
            saveGeneratedDocument(contract, GENERATED_GOODS_EXCEL_ID, fileName, TYPE_GOODS_EXCEL, storedFile, null);
            return success(TYPE_GOODS_EXCEL, storedFile);
        } catch (Exception ex) {
            log.warn("Export goods Excel failed notifyNo={}, error={}", notifyNo, safeMessage(ex));
            saveGeneratedDocument(contract, GENERATED_GOODS_EXCEL_ID, fileName, TYPE_GOODS_EXCEL, null, safeMessage(ex));
            return failed(TYPE_GOODS_EXCEL, safeMessage(ex));
        }
    }

    private void saveGeneratedDocument(
            Contract contract,
            String fileExternalId,
            String fileName,
            String fileType,
            StoredFile storedFile,
            String errorMessage
    ) {
        if (contract == null) {
            return;
        }
        BiddingDocument document = biddingDocumentRepository.findByContractAndFileExternalId(contract, fileExternalId)
                .orElseGet(BiddingDocument::new);
        document.setContract(contract);
        document.setFileExternalId(fileExternalId);
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setSourceType(SOURCE_GENERATED_EXPORT);
        document.setSourceRef(contract.getNotifyNo());
        document.setFileRole(null);
        if (storedFile != null) {
            document.setDownloadStatus(STATUS_SUCCESS);
            document.setStoragePath(storedFile.path());
            document.setFileSize(storedFile.size());
            document.setDownloadedAt(OffsetDateTime.now());
            document.setErrorMessage(null);
        } else {
            document.setDownloadStatus(STATUS_FAILED);
            document.setStoragePath(null);
            document.setFileSize(null);
            document.setDownloadedAt(null);
            document.setErrorMessage(errorMessage);
        }
        biddingDocumentRepository.save(document);
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
