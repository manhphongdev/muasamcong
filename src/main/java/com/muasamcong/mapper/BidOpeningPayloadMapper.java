package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.integration.helper.PortalHelper;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BidOpeningPayloadMapper {
    public Iterable<JsonNode> contractorItems(JsonNode root) {
        if (root == null || !root.isArray()) {
            return java.util.List.of();
        }
        return root;
    }

    public String text(JsonNode node, String field) {
        return PortalHelper.text(node, field);
    }

    public Long longValue(JsonNode node, String field) {
        return PortalHelper.longValue(node, field);
    }

    public Integer integer(JsonNode node, String field) {
        return PortalHelper.integer(node, field);
    }

    public BigDecimal decimal(JsonNode node, String field) {
        return PortalHelper.decimal(node, field);
    }
}
