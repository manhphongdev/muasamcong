package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BiddingResultPayloadMapper {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public JsonNode mainPayload(JsonNode root) {
        JsonNode value = root == null ? null : root.get("bideContractorInputResultDTO");
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Empty bidding result payload");
        }
        return value;
    }

    public List<JsonNode> contractorItems(JsonNode main) {
        List<JsonNode> items = new ArrayList<>();
        JsonNode lots = main == null ? null : main.get("lotResultDTO");
        if (lots == null || !lots.isArray()) {
            return items;
        }

        for (JsonNode lot : lots) {
            JsonNode contractors = lot.get("contractorList");
            if (contractors != null && contractors.isArray()) {
                for (JsonNode contractor : contractors) {
                    items.add(contractor);
                }
            }
        }
        return items;
    }

    public String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
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

    public boolean hasWinner(JsonNode main) {
        for (JsonNode item : contractorItems(main)) {
            Integer bidResult = integer(item, "bidResult");
            if (bidResult != null && bidResult == 1) {
                return true;
            }
        }
        return false;
    }
}
