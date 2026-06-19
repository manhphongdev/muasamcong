package com.muasamcong.service.bidpackage;

import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderCreateResult;
import com.muasamcong.dto.bidpackage.syncrootfolder.BidPackageSyncRootFolderResult;
import java.util.List;

public interface SyncSourceService {
    List<BidPackageSyncRootFolderResult> list();

    BidPackageSyncRootFolderCreateResult create(List<String> paths);

    BidPackageSyncRootFolderResult activate(Long id);

    BidPackageSyncRootFolderResult deactivate(Long id);
}
