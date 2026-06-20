package com.muasamcong.dto;

public record PortalSyncContext(
        String notifyNo,
        String detailUrl,
        BidApiParams apiParams
) {
    public static PortalSyncContext from(String notifyNo, ResolvedBidDetail resolved) {
        return new PortalSyncContext(notifyNo, resolved.detailUrl(), resolved.apiParams());
    }
}
