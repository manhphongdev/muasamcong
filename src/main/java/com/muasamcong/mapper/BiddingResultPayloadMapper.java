package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muasamcong.integration.helper.PortalHelper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BiddingResultPayloadMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public List<GoodsItem> goodsItems(JsonNode main) {
        List<GoodsItem> items = new ArrayList<>();
        JsonNode lots = main == null ? null : main.get("lotResultDTO");
        if (lots == null || !lots.isArray()) {
            return items;
        }

        int fallbackOrder = 1;
        for (JsonNode lot : lots) {
            String winningCode = text(lot, "winningCode");
            String goodsListRaw = text(lot, "goodsList");
            if (winningCode == null || goodsListRaw == null) {
                continue;
            }

            JsonNode goodsList = parseJson(goodsListRaw);
            if (goodsList == null || !goodsList.isArray()) {
                continue;
            }

            for (JsonNode goodsEntry : goodsList) {
                String contractorCode = text(goodsEntry, "contractorCode");
                if (!sameCode(winningCode, contractorCode)) {
                    continue;
                }

                JsonNode rows = goodsEntry.path("formValue").path("lotContent").path("Table");
                if (!rows.isArray()) {
                    continue;
                }
                for (JsonNode row : rows) {
                    Integer sortOrder = firstInteger(row, "currentItemIndex", "pos");
                    items.add(new GoodsItem(lot, goodsEntry, row, sortOrder == null ? fallbackOrder : sortOrder));
                    fallbackOrder++;
                }
            }
        }
        return items;
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

    public BigDecimal decimalValue(JsonNode node, String field) {
        return PortalHelper.decimal(node, field);
    }

    public BigDecimal decimal(JsonNode node, String field) {
        return PortalHelper.decimal(node, field);
    }

    public OffsetDateTime dateTime(JsonNode node, String field) {
        return PortalHelper.dateTime(node, field);
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

    public String firstText(JsonNode node, String... fields) {
        return PortalHelper.firstText(node, fields);
    }

    public Integer firstInteger(JsonNode node, String... fields) {
        for (String field : fields) {
            Integer value = integer(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public Long firstLong(JsonNode node, String... fields) {
        for (String field : fields) {
            Long value = longValue(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public BigDecimal firstDecimal(JsonNode node, String... fields) {
        for (String field : fields) {
            BigDecimal value = decimalValue(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean sameCode(String first, String second) {
        return normalizeCode(first).equalsIgnoreCase(normalizeCode(second));
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.regionMatches(true, 0, "vn", 0, 2) ? normalized.substring(2) : normalized;
    }

    public record GoodsItem(JsonNode lot, JsonNode goodsEntry, JsonNode row, Integer sortOrder) {
    }
}
