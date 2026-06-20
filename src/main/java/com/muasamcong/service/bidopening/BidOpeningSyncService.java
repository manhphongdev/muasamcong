package com.muasamcong.service.bidopening;

import com.muasamcong.dto.PortalSyncContext;
import com.muasamcong.dto.bidopening.BidOpeningSyncResult;

public interface BidOpeningSyncService {
    BidOpeningSyncResult sync(PortalSyncContext context);
}
