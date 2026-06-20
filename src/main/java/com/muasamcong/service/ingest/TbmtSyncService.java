package com.muasamcong.service.ingest;

import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.dto.PortalSyncContext;

public interface TbmtSyncService {
    TbmtIngestResult sync(PortalSyncContext context);
}
