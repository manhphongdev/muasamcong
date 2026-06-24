package com.muasamcong.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sync.bid-package")
@Getter
@Setter
public class BidPackageSyncProperties {
    private boolean enabled = true;
    private long fixedDelayMs = 60000;
    private int documentDownloadLimit = 50;
}
