package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.integration.helper.PortalHelper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class TbmtPayloadMapper {
    public JsonNode mainPayload(JsonNode root) {
        if (root == null || root.isNull()) {
            throw new IllegalArgumentException("Empty TBMT payload");
        }

        JsonNode main = root.get("bidoNotifyContractorM");
        if (main != null && !main.isNull()) {
            return main;
        }

        return root;
    }

    public String text(JsonNode node, String field) {
        return PortalHelper.text(node, field);
    }

    public Integer integer(JsonNode node, String field) {
        return PortalHelper.integer(node, field);
    }

    public Long longValue(JsonNode node, String field) {
        return PortalHelper.longValue(node, field);
    }

    public BigDecimal decimal(JsonNode node, String field) {
        return PortalHelper.decimal(node, field);
    }

    public Boolean booleanValue(JsonNode node, String field) {
        return PortalHelper.booleanValue(node, field);
    }

    public OffsetDateTime dateTime(JsonNode node, String field) {
        return PortalHelper.dateTime(node, field);
    }
}
