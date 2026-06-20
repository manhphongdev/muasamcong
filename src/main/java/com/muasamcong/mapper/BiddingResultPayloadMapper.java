package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muasamcong.dto.biddingresult.BiddingResultContractorPayload;
import com.muasamcong.dto.biddingresult.BiddingResultGoodsPayload;
import com.muasamcong.dto.biddingresult.BiddingResultSummaryPayload;
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

    public List<BiddingResultContractorPayload> contractors(JsonNode main) {
        return contractorItems(main).stream()
                .map(this::toContractorPayload)
                .toList();
    }

    private BiddingResultContractorPayload toContractorPayload(JsonNode item) {
        return BiddingResultContractorPayload.builder()
                .contractorCode(text(item, "orgCode"))
                .contractorName(text(item, "orgFullname"))
                .taxCode(text(item, "taxCode"))
                .bidResult(integer(item, "bidResult"))
                .winningPrice(longValue(item, "bidWiningPrice"))
                .reason(text(item, "reason"))
                .lotPrice(longValue(item, "lotPrice"))
                .lotFinalPrice(longValue(item, "lotFinalPrice"))
                .adjustedPrice(longValue(item, "adjustPrice"))
                .evalPrice(longValue(item, "evalBidPrice"))
                .techScore(decimal(item, "techScore"))
                .discountRate(decimal(item, "discountPercent"))
                .contractPeriod(integer(item, "cperiod"))
                .contractPeriodUnit(text(item, "cperiodUnit"))
                .contractPeriodText(text(item, "cperiodText"))
                .contractExecutionTime(text(item, "bidExecutionTime"))
                .otherContent(text(item, "otherContent"))
                .build();
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

    public List<BiddingResultGoodsPayload> goods(JsonNode main) {
        return goodsItems(main).stream()
                .map(item -> toGoodsPayload(main, item))
                .toList();
    }

    private BiddingResultGoodsPayload toGoodsPayload(JsonNode main, GoodsItem item) {
        return BiddingResultGoodsPayload.builder()
                .contractorCode(text(item.goodsEntry(), "contractorCode"))
                .notifyNo(text(main, "notifyNo"))
                .bidName(text(main, "bidName"))
                .lotNo(firstNonBlank(text(item.lot(), "lotNo"), text(item.goodsEntry(), "lotNo")))
                .lotName(text(item.lot(), "lotName"))
                .goodsName(text(item.row(), "name"))
                .goodsCode(text(item.row(), "codeGood"))
                .goodsLabel(firstText(item.row(), "labelGood", "lableGood"))
                .yearManufacture(firstText(item.row(), "yearManufacture", "yearGood"))
                .origin(text(item.row(), "origin"))
                .manufacturer(text(item.row(), "manufacturer"))
                .technicalFeatures(firstText(item.row(), "feature", "technique"))
                .unit(text(item.row(), "uom"))
                .quantity(firstDecimal(item.row(), "qty", "originQty"))
                .hsCode(firstText(item.row(), "hsCode", "maHs", "maHS"))
                .winningUnitPrice(firstLong(item.row(), "bidPrice"))
                .amount(firstLong(item.row(), "amount"))
                .deliveryTime(firstText(item.row(), "cPeriod", "cperiod", "deliveryTime"))
                .sortOrder(item.sortOrder())
                .rawItem(item.row().toString())
                .build();
    }

    public BiddingResultSummaryPayload summary(JsonNode main) {
        return BiddingResultSummaryPayload.builder()
                .resultVersion(text(main, "resultVersion"))
                .notifyVersion(text(main, "notifyVersion"))
                .resultStatus(text(main, "status"))
                .publicDate(dateTime(main, "publicDate"))
                .decisionNo(text(main, "decisionNo"))
                .decisionDate(dateTime(main, "decisionDate"))
                .decisionAgency(text(main, "decisionAgency"))
                .decisionFileId(text(main, "decisionFileId"))
                .decisionFileName(text(main, "decisionFileName"))
                .evalReportFileInfo(text(main, "evalReportFileInfo"))
                .hasWinner(hasWinner(main))
                .build();
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

    private String firstNonBlank(String first, String second) {
        return PortalHelper.isBlank(first) ? second : first;
    }

    public record GoodsItem(JsonNode lot, JsonNode goodsEntry, JsonNode row, Integer sortOrder) {
    }
}
