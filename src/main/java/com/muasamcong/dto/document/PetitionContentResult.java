package com.muasamcong.dto.document;

import java.time.OffsetDateTime;

public record PetitionContentResult(
        String petitionPeriod,
        String reqContent,
        String resContent,
        OffsetDateTime reqDate,
        OffsetDateTime resDate,
        Boolean replied,
        String restrictCompetition,
        Integer sortOrder
) {
}
