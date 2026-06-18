package com.muasamcong.dto.bidpackage;

public record ScrapingLogDto(
        String timestamp,
        String level,
        String stage,
        String message
) {}
