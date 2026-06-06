package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
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
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public Long longValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isNumber()) {
            return value.asLong();
        }

        try {
            return Long.valueOf(value.asText());
        } catch (Exception ex) {
            return null;
        }
    }

    public Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isNumber()) {
            return value.asInt();
        }

        try {
            return Integer.valueOf(value.asText());
        } catch (Exception ex) {
            return null;
        }
    }

    public BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        try {
            return new BigDecimal(value.asText());
        } catch (Exception ex) {
            return null;
        }
    }
}
