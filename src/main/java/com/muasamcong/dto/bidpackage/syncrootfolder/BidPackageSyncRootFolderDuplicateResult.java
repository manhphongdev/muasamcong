package com.muasamcong.dto.bidpackage.syncrootfolder;

public record BidPackageSyncRootFolderDuplicateResult(
        String notifyNo,
        String folderName,
        String newPath,
        String existingPath,
        String message
) {
}
