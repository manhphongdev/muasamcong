package com.muasamcong.dto.bidpackage.syncsystem;

import java.util.List;

public record BidPackageSyncSystemUpdateRequest(
        Boolean enabled,
        Integer intervalMinutes,
        List<String> importRootPaths
) {
}
