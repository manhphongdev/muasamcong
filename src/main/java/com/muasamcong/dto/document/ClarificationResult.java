package com.muasamcong.dto.document;

import java.time.OffsetDateTime;
import java.util.List;

public record ClarificationResult(
        String reqNo,
        String reqName,
        OffsetDateTime reqDate,
        OffsetDateTime signReqDate,
        OffsetDateTime signResDate,
        List<ClarificationContentResult> contents,
        List<DocumentFileResult> files
) {
}
