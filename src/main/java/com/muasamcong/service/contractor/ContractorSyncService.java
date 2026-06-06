package com.muasamcong.service.contractor;

import com.muasamcong.dto.contractor.ContractorSyncResult;

public interface ContractorSyncService {
    ContractorSyncResult syncContractors(int pageSize);
}
