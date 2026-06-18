package com.muasamcong.dto.bidpackage.syncrootfolder;

import java.util.List;

public record BidPackageSyncRootFolderCreateRequest(
        List<String> paths
) {
}
