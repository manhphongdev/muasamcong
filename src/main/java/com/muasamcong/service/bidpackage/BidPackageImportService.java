package com.muasamcong.service.bidpackage;

import com.muasamcong.dto.bidpackage.BidPackageFolderImportRequest;
import com.muasamcong.dto.bidpackage.BidPackageFolderImportResult;

public interface BidPackageImportService {
    BidPackageFolderImportResult importFolders(BidPackageFolderImportRequest request);
}
