package com.muasamcong.service.investor;

import com.muasamcong.dto.investor.InvestorSyncResult;

public interface InvestorSyncService {
    InvestorSyncResult syncInvestors(int pageSize);
}
