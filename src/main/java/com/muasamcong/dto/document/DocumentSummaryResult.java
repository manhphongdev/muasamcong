package com.muasamcong.dto.document;

import java.util.List;
import java.util.Map;

public record DocumentSummaryResult(
        String notifyNo,
        int total,
        int pending,
        int downloading,
        int success,
        int failed,
        double successRate,
        Map<String, Long> byFileType,
        List<DocumentFileResult> files
) {
}
