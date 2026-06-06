package com.muasamcong.dto.bidpackage;

public record BidPackageFolderImportResult(
        int totalFolders,
        int created,
        int existed,
        int invalid,
        int failed
) {
}
