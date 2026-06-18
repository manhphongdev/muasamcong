package com.muasamcong.dto.bidpackage;

public record MissingFieldDto(
        String field,
        String source,
        String reason,
        String suggestion
) {}
