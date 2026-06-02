package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalTbmtClient {
    private static final URI TBMT_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-contractor-selection-v2/services/lcnt_tbmt_ttc_ldt?token=fake"
    );

    private final PortalJsonClient jsonClient;

    public JsonNode fetchTbmt(String notifyId) {
        if (notifyId == null || notifyId.isBlank()) {
            throw new IllegalArgumentException("notifyId is required");
        }
        return jsonClient.postJson(TBMT_URI, "{\"id\":" + jsonClient.quote(notifyId) + "}");
    }
}
