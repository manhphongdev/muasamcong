package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalContractor {
    private static final URI CONTRACTOR_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-contractors-approved/services/get-list"
    );

    private final PortalJson jsonClient;

    public JsonNode fetchContractors(int pageNumber, int pageSize) {
        String body = """
                {
                  "pageSize": %d,
                  "pageNumber": %d,
                  "queryParams": {
                    "officePro": { "contains": null },
                    "effRoleDate": {
                      "greaterThanOrEqual": null,
                      "lessThanOrEqual": null
                    },
                    "isForeignInvestor": { "equals": null },
                    "roleType": { "in": ["NT", "NTPER"] },
                    "decNo": { "contains": null },
                    "orgName": { "contains": null },
                    "taxCode": { "contains": null },
                    "orgNameOrOrgCode": { "contains": null },
                    "agencyName": { "in": null }
                  }
                }
                """.formatted(pageSize, pageNumber);

        return jsonClient.postJson(CONTRACTOR_URI, body);
    }
}
