package com.muasamcong.dto.export;

public record AutoDownloadExportItemResult(
        String type,
        boolean success,
        String path,
        Long size,
        String message
) {
}
