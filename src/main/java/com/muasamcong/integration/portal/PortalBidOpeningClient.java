package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalBidOpeningClient {
    private static final URI LOT_OPEN_DETAIL_URI = URI.create(
            "https://muasamcong.mpi.gov.vn/o/egp-portal-contractor-selection-v2/services/expose/ldtkqmt/bid-notification-p/lotOpenDetail?token=fake"
    );

    private final PortalJsonClient jsonClient;

    public JsonNode fetchLotOpenDetail(BidApiParams params) {
        String bidOpenId = params.bidOpenId() == null || params.bidOpenId().isBlank()
                ? params.bidPreOpenId()
                : params.bidOpenId();

        String body = "{"
                + "\"notifyNo\":" + jsonClient.quote(params.notifyNo()) + ","
                + "\"type\":\"TBMT\","
                + "\"packType\":1,"
                + "\"viewType\":0,"
                + "\"notifyId\":" + jsonClient.quote(params.notifyId()) + ","
                + "\"bidOpenId\":" + jsonClient.quote(bidOpenId)
                + "}";

        return jsonClient.postJson(LOT_OPEN_DETAIL_URI, body);
    }
}
