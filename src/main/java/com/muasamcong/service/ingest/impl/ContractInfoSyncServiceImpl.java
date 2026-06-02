package com.muasamcong.service.ingest.impl;

import com.muasamcong.dto.ContractInfoSyncResult;
import com.muasamcong.model.Contract;
import com.muasamcong.repository.ContractRepository;
import java.util.List;

import com.muasamcong.service.ingest.ContractInfoSyncService;
import com.muasamcong.service.ingest.TbmtSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractInfoSyncServiceImpl implements ContractInfoSyncService {
    private final ContractRepository contractRepository;
    private final TbmtSyncService tbmtSyncService;

    @Override
    public ContractInfoSyncResult syncFromContracts() {
        List<Contract> contracts = contractRepository.findByNotifyNoIsNotNull();
        int synced = 0;
        int failed = 0;

        for (Contract contract : contracts) {
            try {
                String notifyNo = contract.getNotifyNo();
                if (notifyNo == null || notifyNo.isBlank()) {
                    continue;
                }
                tbmtSyncService.syncByNotifyNo(notifyNo);
                synced++;
            } catch (Exception ex) {
                failed++;
            }
        }

        return new ContractInfoSyncResult(contracts.size(), synced, failed);
    }
}
