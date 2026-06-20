package com.muasamcong.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.TbmtPayload;
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

    public TbmtPayload toPayload(JsonNode root) {
        JsonNode tbmt = mainPayload(root);
        JsonNode bidStatus = root == null ? null : root.get("bidoBidStatus");

        return TbmtPayload.builder()
                .notifyVersion(text(tbmt, "notifyVersion"))
                .businessStatus(text(tbmt, "status"))
                .bidName(text(tbmt, "bidName"))
                .capitalDetail(text(tbmt, "capitalDetail"))
                .investField(text(tbmt, "investField"))
                .bidForm(text(tbmt, "bidForm"))
                .contractType(text(tbmt, "contractType"))
                .bidMode(text(tbmt, "bidMode"))
                .contractPeriod(integer(tbmt, "contractPeriod"))
                .contractPeriodUnit(text(tbmt, "contractPeriodUnit"))
                .multiLot(resolveMultiLot(tbmt))
                .domestic(booleanValue(tbmt, "isDomestic"))
                .bidPrice(longValue(tbmt, "bidPrice"))
                .bidPriceUnit(text(tbmt, "bidPriceUnit"))
                .bidEstimatePrice(longValue(tbmt, "bidEstimatePrice"))
                .bidValidityPeriod(integer(tbmt, "bidValidityPeriod"))
                .bidValidityPeriodUnit(text(tbmt, "bidValidityPeriodUnit"))
                .prequalification(booleanValue(tbmt, "isPrequalification"))
                .internet(booleanValue(tbmt, "isInternet"))
                .submissionMethod(text(tbmt, "submissionMethod"))
                .issueLocation(text(tbmt, "issueLocation"))
                .receiveLocation(text(tbmt, "receiveLocation"))
                .executionLocation(text(tbmt, "executionLocation"))
                .feeType(text(tbmt, "feeType"))
                .feeValue(decimal(tbmt, "feeValue"))
                .feeUnit(text(tbmt, "feeUnit"))
                .bidCloseAt(dateTime(tbmt, "bidCloseDate"))
                .bidOpenAt(dateTime(tbmt, "bidOpenDate"))
                .bidOpenLocation(text(tbmt, "bidOpenLocation"))
                .guaranteeValue(decimal(tbmt, "guaranteeValue"))
                .guaranteeUnit(text(tbmt, "guaranteeUnit"))
                .guaranteeForm(text(tbmt, "guaranteeForm"))
                .bidOpeningCompletedAt(dateTime(bidStatus, "successBidOpenDate"))
                .investorCode(text(tbmt, "investorCode"))
                .investorName(text(tbmt, "investorName"))
                .oldInvestorName(text(tbmt, "oldInvestorName"))
                .mergeInvestorDate(dateTime(tbmt, "mergeInvestorDate"))
                .planNo(text(tbmt, "planNo"))
                .planName(text(tbmt, "planName"))
                .build();
    }

    private Boolean resolveMultiLot(JsonNode tbmt) {
        Boolean value = booleanValue(tbmt, "isMultiLot");
        if (value != null) {
            return value;
        }
        JsonNode lotList = tbmt == null ? null : tbmt.get("lotDTOList");
        return lotList != null && lotList.isArray() && lotList.size() > 1;
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
