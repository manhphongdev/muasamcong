package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class TbmtPayloadMapper {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return text == null || text.isBlank() || "undefined".equalsIgnoreCase(text.trim()) ? null : text.trim();
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

    public Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        String raw = value.asText();
        if ("1".equals(raw)) {
            return true;
        }
        if ("0".equals(raw)) {
            return false;
        }

        return Boolean.parseBoolean(raw);
    }

    public OffsetDateTime dateTime(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(value).atZone(ZONE).toOffsetDateTime();
        } catch (Exception ignored) {
        }

        return null;
    }
}
