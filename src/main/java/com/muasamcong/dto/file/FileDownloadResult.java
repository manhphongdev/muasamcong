package com.muasamcong.dto.file;

public record FileDownloadResult(
        String status,
        String fileId,
        String fileName,
        String path,
        String storagePath,
        long size
) {
}
