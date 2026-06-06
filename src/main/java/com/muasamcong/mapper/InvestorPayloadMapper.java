package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.enums.OperatingStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InvestorPayloadMapper {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public JsonNode content(JsonNode root) {
        JsonNode content = root == null ? null : root.get("content");
        return content != null && content.isArray() ? content : null;
    }

    public long totalElements(JsonNode root) {
        JsonNode value = root == null ? null : root.get("totalElements");
        return value != null && value.isNumber() ? value.asLong() : 0;
    }

    public int totalPages(JsonNode root) {
        JsonNode value = root == null ? null : root.get("totalPages");
        return value != null && value.isNumber() ? value.asInt() : 0;
    }

    public String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public List<String> taxCodes(JsonNode node) {
        JsonNode value = node == null ? null : node.get("taxCode");
        if (value == null || value.isNull()) {
            return List.of();
        }

        if (value.isArray()) {
            return normalizeTaxCodes(value);
        }

        return normalizeTaxCodes(value.asText());
    }

    public OperatingStatus operatingStatus(JsonNode node) {
        JsonNode value = node == null ? null : node.get("status");
        if (value == null || value.isNull()) {
            return OperatingStatus.UNKNOWN;
        }

        return switch (value.asInt()) {
            case 1 -> OperatingStatus.ACTIVE;
            case 2 -> OperatingStatus.INACTIVE;
            default -> OperatingStatus.UNKNOWN;
        };
    }

    public OffsetDateTime approvedAt(JsonNode node) {
        JsonNode value = node == null ? null : node.get("effRoleDate");
        if (value == null || !value.isArray() || value.size() < 3) {
            return null;
        }

        int year = value.get(0).asInt();
        int month = value.get(1).asInt();
        int day = value.get(2).asInt();
        int hour = value.size() > 3 ? value.get(3).asInt() : 0;
        int minute = value.size() > 4 ? value.get(4).asInt() : 0;
        int second = value.size() > 5 ? value.get(5).asInt() : 0;

        return LocalDateTime.of(year, month, day, hour, minute, second)
                .atZone(ZONE)
                .toOffsetDateTime();
    }

    private List<String> normalizeTaxCodes(JsonNode values) {
        List<String> rawValues = new java.util.ArrayList<>();
        for (JsonNode value : values) {
            if (value != null && !value.isNull()) {
                rawValues.add(value.asText());
            }
        }
        return normalizeTaxCodes(rawValues.toArray(String[]::new));
    }

    private List<String> normalizeTaxCodes(String value) {
        return value == null ? List.of() : normalizeTaxCodes(value.split("[,;]"));
    }

    private List<String> normalizeTaxCodes(String[] values) {
        return Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
