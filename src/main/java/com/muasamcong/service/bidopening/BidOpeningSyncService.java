package com.muasamcong.service.bidopening;

import com.muasamcong.dto.bidopening.BidOpeningSyncResult;

public interface BidOpeningSyncService {
    BidOpeningSyncResult syncByNotifyNo(String notifyNo);
}
