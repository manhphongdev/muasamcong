package com.muasamcong.service.biddingresult;

import com.muasamcong.dto.PortalSyncContext;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;

public interface BiddingResultSyncService {
    BiddingResultSyncResult sync(PortalSyncContext context);
}
