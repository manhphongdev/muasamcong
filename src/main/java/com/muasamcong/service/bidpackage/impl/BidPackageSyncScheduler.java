package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.service.bidpackage.SyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidPackageSyncScheduler {
    private final SyncJobService syncJobService;

    @Scheduled(fixedDelayString = "${sync.bid-package.fixed-delay-ms:60000}")
    public void tick() {
        try {
            syncJobService.runScheduledIfDue();
        } catch (Exception ex) {
            log.warn("Bid package sync scheduler tick failed error={}", ex.getMessage());
        }
    }
}
