package com.muasamcong.dto.bidpackage;

import java.util.List;

public record BidPackageFolderImportResult(
        int totalFolders,
        int created,
        int existed,
        int invalid,
        int failed,
        String message,
        List<String> existedNotifyNos
) {
}
