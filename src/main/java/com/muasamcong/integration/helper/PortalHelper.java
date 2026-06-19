package com.muasamcong.integration.helper;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class PortalHelper {
    private static final ZoneId PORTAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private PortalHelper() {
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return isBlank(text) || "undefined".equalsIgnoreCase(text.trim()) ? null : text.trim();
    }

    public static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static Integer integer(JsonNode node, String field) {
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

    public static Long longValue(JsonNode node, String field) {
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

    public static BigDecimal decimal(JsonNode node, String field) {
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

    public static Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        String raw = text(node, field);
        if (raw == null) {
            return null;
        }
        if ("1".equals(raw)) {
            return true;
        }
        if ("0".equals(raw)) {
            return false;
        }

        return Boolean.parseBoolean(raw);
    }

    public static OffsetDateTime dateTime(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(value).atZone(PORTAL_ZONE).toOffsetDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
