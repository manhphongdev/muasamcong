package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalInvestorClient {
    private static final URI INVESTOR_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-investors-approved/services/get-list-approve-bidder"
    );

    private final PortalJsonClient jsonClient;

    public JsonNode fetchInvestors(int pageNumber, int pageSize) {
        String body = """
                {
                  "pageSize": %d,
                  "pageNumber": %d,
                  "queryParams": {
                    "roleType": { "equals": "NDT" },
                    "orgName": { "contains": null },
                    "taxCode": { "contains": null },
                    "isForeignInvestor": { "equals": null },
                    "decNo": { "contains": null },
                    "effRoleDate": {
                      "greaterThanOrEqual": null,
                      "lessThanOrEqual": null
                    },
                    "officePro": { "equals": null },
                    "orgNameOrOrgCode": { "contains": null }
                  }
                }
                """.formatted(pageSize, pageNumber);

        return jsonClient.postJson(INVESTOR_URI, body);
    }
}
