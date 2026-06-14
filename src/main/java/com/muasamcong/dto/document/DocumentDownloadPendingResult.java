package com.muasamcong.dto.document;

public record DocumentDownloadPendingResult(
        int total,
        int success,
        int failed,
        int skipped
) {
}
