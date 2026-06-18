package com.muasamcong.dto.file;

import jakarta.validation.constraints.NotBlank;

public record FileDownloadRequest(
        @NotBlank String fileId,
        @NotBlank String fileName,
        @NotBlank String basePath,
        String relativePath
) {
}
