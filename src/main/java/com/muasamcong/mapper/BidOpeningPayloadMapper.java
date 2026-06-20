package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.bidopening.BidOpeningContractorPayload;
import com.muasamcong.integration.helper.PortalHelper;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;

@Component
public class BidOpeningPayloadMapper {
    public Iterable<JsonNode> contractorItems(JsonNode root) {
        if (root == null || !root.isArray()) {
            return List.of();
        }
        return root;
    }

    public List<BidOpeningContractorPayload> contractors(JsonNode root) {
        return StreamSupport.stream(contractorItems(root).spliterator(), false)
                .map(this::toContractorPayload)
                .toList();
    }

    private BidOpeningContractorPayload toContractorPayload(JsonNode item) {
        return BidOpeningContractorPayload.builder()
                .contractorCode(text(item, "contractorCode"))
                .contractorName(text(item, "contractorName"))
                .bidPrice(longValue(item, "lotPrice"))
                .discountRate(decimal(item, "discountPercent"))
                .bidPriceAfterDiscount(longValue(item, "lotFinalPrice"))
                .bidGuaranteeAmount(longValue(item, "bidGuaranteeAmount"))
                .bidGuaranteeValidityPeriod(integer(item, "bidGuaranteeEff"))
                .contractExecutionTime(text(item, "cperiodText"))
                .build();
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
