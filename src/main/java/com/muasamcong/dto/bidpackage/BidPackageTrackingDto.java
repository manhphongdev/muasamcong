package com.muasamcong.dto.bidpackage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record BidPackageTrackingDto(
        String notifyNo,
        String title,
        String investor,
        String folderName,
        String folderPath,
        Boolean folderExists,
        Long budget,
        String publishDate,
        OffsetDateTime closeTime,
        OffsetDateTime openTime,
        String executionTime,
        Long winningPrice,
        String winningContractor,
        String bidUrl,
        String lifecycle,
        String scrapeStatus,
        OffsetDateTime lastSyncTime,
        String lastMessage,
        Integer completenessPercent,
        Integer downloadCompletenessPercent,
        Integer documentTotal,
        Integer documentDownloaded,
        Integer documentFailed,
        Integer documentPending,
        Map<String, String> apiCoverage,
        List<BidderDto> bidders,
        List<ScrapingLogDto> logs,
        List<MissingFieldDto> missingFields,
        Integer retryCount
) {}
