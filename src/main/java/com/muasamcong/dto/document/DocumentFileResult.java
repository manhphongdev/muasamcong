package com.muasamcong.dto.document;

public record DocumentFileResult(
        Long id,
        String fileId,
        String fileName,
        String fileType,
        String downloadStatus,
        String storagePath,
        Long fileSize,
        String errorMessage
) {
}
