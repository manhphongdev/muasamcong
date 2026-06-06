package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalBiddingResultClient {
    private static final URI BIDDING_RESULT_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-contractor-selection-v2/services/expose/contractor-input-result/get?token=fake"
    );

    private final PortalJsonClient jsonClient;

    public JsonNode fetchBiddingResult(String inputResultId) {
        if (inputResultId == null || inputResultId.isBlank()) {
            throw new IllegalArgumentException("inputResultId is required");
        }
        return jsonClient.postJson(BIDDING_RESULT_URI, "{\"id\":" + jsonClient.quote(inputResultId) + "}");
    }
}
