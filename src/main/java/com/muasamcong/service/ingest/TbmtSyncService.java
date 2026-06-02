package com.muasamcong.service.ingest;

import com.muasamcong.dto.TbmtIngestResult;

public interface TbmtSyncService {
    TbmtIngestResult syncByNotifyNo(String notifyNo);
}
