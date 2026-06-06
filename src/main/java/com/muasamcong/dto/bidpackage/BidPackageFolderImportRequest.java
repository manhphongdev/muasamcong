package com.muasamcong.dto.bidpackage;

import java.util.List;

public record BidPackageFolderImportRequest(
        List<String> folderPaths
) {
}
