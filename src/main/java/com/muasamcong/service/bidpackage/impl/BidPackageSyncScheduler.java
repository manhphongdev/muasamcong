package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.service.bidpackage.BidPackageSyncSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidPackageSyncScheduler {
    private final BidPackageSyncSystemService syncSystemService;

    @Scheduled(fixedDelay = 60000)
    public void tick() {
        try {
            syncSystemService.runScheduledIfDue();
        } catch (Exception ex) {
            log.warn("Bid package sync scheduler tick failed error={}", ex.getMessage());
        }
    }
}
