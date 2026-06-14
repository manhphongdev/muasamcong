package com.muasamcong.dto.document;

import java.time.OffsetDateTime;
import java.util.List;

public record PetitionResult(
        String reqNo,
        String reqName,
        String petitionPeriod,
        OffsetDateTime reqDate,
        OffsetDateTime resDate,
        String contractorCode,
        String contractorName,
        List<PetitionContentResult> contents,
        List<DocumentFileResult> files
) {
}
