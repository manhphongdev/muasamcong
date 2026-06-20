package com.muasamcong.mapper;

import static com.muasamcong.integration.helper.PortalHelper.firstText;
import static com.muasamcong.integration.helper.PortalHelper.text;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import org.springframework.stereotype.Component;

@Component
public class PortalSearchPayloadMapper {
    public BidApiParams toBidApiParams(JsonNode match) {
        String notifyNo = text(match, "notifyNo");
        String id = firstText(match, "id", "notifyId");
        String notifyId = firstText(match, "notifyId", "id");
        String stepCode = firstText(match, "stepCode", "step");

        return new BidApiParams(
                notifyNo,
                id,
                notifyId,
                text(match, "inputResultId"),
                text(match, "bidOpenId"),
                text(match, "techReqId"),
                text(match, "bidPreNotifyResultId"),
                text(match, "bidPreOpenId"),
                text(match, "processApply"),
                text(match, "bidMode"),
                text(match, "bidForm"),
                text(match, "planNo"),
                stepCode,
                text(match, "isInternet")
        );
    }
}
