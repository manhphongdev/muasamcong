package com.muasamcong.service.export;

import com.muasamcong.dto.export.AutoDownloadExportResult;

public interface AutoDownloadExportService {
    AutoDownloadExportResult exportGeneratedFiles(String notifyNo, String sourcePath);
}
