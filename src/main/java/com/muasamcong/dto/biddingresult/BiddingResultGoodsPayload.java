package com.muasamcong.dto.biddingresult;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BiddingResultGoodsPayload(
        String contractorCode,
        String notifyNo,
        String bidName,
        String lotNo,
        String lotName,
        String goodsName,
        String goodsCode,
        String goodsLabel,
        String yearManufacture,
        String origin,
        String manufacturer,
        String technicalFeatures,
        String unit,
        BigDecimal quantity,
        String hsCode,
        Long winningUnitPrice,
        Long amount,
        String deliveryTime,
        Integer sortOrder,
        String rawItem
) {
}
