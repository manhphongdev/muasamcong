package com.muasamcong.dto.export;

import java.util.List;

public record AutoDownloadExportResult(
        int total,
        int success,
        int failed,
        int skipped,
        List<AutoDownloadExportItemResult> items
) {
}
