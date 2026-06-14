package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalDocumentClient {
    private static final URI CLARIFICATION_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-contractor-selection-v2/services/lcnt_tbmt_yclr?token=fake"
    );
    private static final URI PETITION_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-contractor-selection-v2/services/lcnt_tbmt_kn?token=fake"
    );

    private final PortalJsonClient jsonClient;

    public JsonNode fetchClarifications(String notifyNo, String processApply) {
        return jsonClient.postJson(CLARIFICATION_URI, body(notifyNo, processApply));
    }

    public JsonNode fetchPetitions(String notifyNo, String processApply) {
        return jsonClient.postJson(PETITION_URI, body(notifyNo, processApply));
    }

    private String body(String notifyNo, String processApply) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        String resolvedProcessApply = processApply == null || processApply.isBlank() ? "LDT" : processApply;
        return "{"
                + "\"notifyNo\":" + jsonClient.quote(notifyNo.trim()) + ","
                + "\"processApply\":" + jsonClient.quote(resolvedProcessApply)
                + "}";
    }
}
