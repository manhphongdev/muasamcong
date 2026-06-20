package com.muasamcong.service.biddingresult.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.PortalSyncContext;
import com.muasamcong.dto.biddingresult.BiddingResultContractorPayload;
import com.muasamcong.dto.biddingresult.BiddingResultGoodsPayload;
import com.muasamcong.dto.biddingresult.BiddingResultSummaryPayload;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;
import com.muasamcong.dto.document.DocumentEnqueueStats;
import com.muasamcong.integration.portal.PortalBiddingResult;
import com.muasamcong.mapper.BiddingResultPayloadMapper;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.BiddingResult;
import com.muasamcong.model.BiddingResultGoods;
import com.muasamcong.model.BiddingResultSummary;
import com.muasamcong.model.Contract;
import com.muasamcong.model.Contractor;
import com.muasamcong.repository.BidOpeningRepository;
import com.muasamcong.repository.BiddingContractorRepository;
import com.muasamcong.repository.BiddingResultGoodsRepository;
import com.muasamcong.repository.BiddingResultRepository;
import com.muasamcong.repository.BiddingResultSummaryRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.ContractorRepository;
import com.muasamcong.service.biddingresult.BiddingResultSyncService;
import com.muasamcong.service.document.BiddingDocumentService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiddingResultSyncServiceImpl implements BiddingResultSyncService {
    private final PortalBiddingResult portalBiddingResultClient;
    private final BiddingResultPayloadMapper mapper;
    private final ContractRepository contractRepository;
    private final BidOpeningRepository bidOpeningRepository;
    private final ContractorRepository contractorRepository;
    private final BiddingContractorRepository biddingContractorRepository;
    private final BiddingResultRepository biddingResultRepository;
    private final BiddingResultGoodsRepository biddingResultGoodsRepository;
    private final BiddingResultSummaryRepository summaryRepository;
    private final BiddingDocumentService biddingDocumentService;

    @Override
    @Transactional
    public BiddingResultSyncResult sync(PortalSyncContext context) {
        String normalizedNotifyNo = normalizeNotifyNo(context.notifyNo());
        log.info("Sync bidding result start notifyNo={}", normalizedNotifyNo);

        BidApiParams params = context.apiParams();
        if (isBlank(params.inputResultId())) {
            throw new IllegalStateException("Cannot resolve inputResultId: " + normalizedNotifyNo);
        }

        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));
        contract.setBidUrl(context.detailUrl());
        BidOpening bidOpening = bidOpeningRepository.findByContract(contract)
                .orElseThrow(() -> new IllegalArgumentException("BidOpening not found: " + normalizedNotifyNo));

        JsonNode root = portalBiddingResultClient.fetchBiddingResult(params.inputResultId());
        JsonNode main = mapper.mainPayload(root);
        boolean hasContractorSelectionResult = main != null
                && !main.isMissingNode()
                && !main.isNull()
                && main.size() > 0;
        BiddingResultSummary summary = upsertSummary(contract, main);
        DocumentEnqueueStats enqueuedDocuments = biddingDocumentService.enqueueBiddingResultFiles(contract, main);

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;
        Map<String, BiddingResult> resultByContractorCode = new HashMap<>();

        for (BiddingResultContractorPayload contractorPayload : mapper.contractors(main)) {
            String contractorCode = contractorPayload.contractorCode();
            String contractorName = contractorPayload.contractorName();
            if (isBlank(contractorCode) || isBlank(contractorName)) {
                skipped++;
                continue;
            }

            Contractor contractor = upsertContractor(contractorCode, contractorName, contractorPayload.taxCode());
            BiddingContractor biddingContractor = biddingContractorRepository
                    .findByBidOpeningAndContractor(bidOpening, contractor)
                    .orElse(null);
            if (biddingContractor == null) {
                skipped++;
                continue;
            }

            BiddingResult result = biddingResultRepository.findByBiddingContractor(biddingContractor).orElse(null);
            boolean newResult = result == null;
            if (newResult) {
                result = new BiddingResult();
                result.setBiddingContractor(biddingContractor);
            }

            boolean changed = applyResultData(result, contractorPayload);
            if (newResult || changed) {
                result.setFetchedAt(OffsetDateTime.now());
                result = biddingResultRepository.save(result);
                if (newResult) {
                    created++;
                } else {
                    updated++;
                }
            } else {
                unchanged++;
            }
            resultByContractorCode.put(normalizeCode(contractorCode), result);
        }
        syncGoods(contract, summary, main, resultByContractorCode);

        log.info("Sync bidding result done notifyNo={}, created={}, updated={}, unchanged={}, skipped={}, foundDocuments={}, newDocuments={}, existingDocuments={}",
                normalizedNotifyNo, created, updated, unchanged, skipped,
                enqueuedDocuments.found(), enqueuedDocuments.created(), enqueuedDocuments.existing());
        return new BiddingResultSyncResult(
                normalizedNotifyNo,
                summary.getId(),
                created,
                updated,
                unchanged,
                skipped,
                hasContractorSelectionResult,
                enqueuedDocuments.found(),
                enqueuedDocuments.created(),
                enqueuedDocuments.existing()
        );
    }

    private BiddingResultSummary upsertSummary(Contract contract, JsonNode main) {
        BiddingResultSummary summary = summaryRepository.findByContract(contract).orElse(null);
        if (summary == null) {
            summary = new BiddingResultSummary();
            summary.setContract(contract);
        }

        BiddingResultSummaryPayload payload = mapper.summary(main);
        summary.setResultVersion(payload.resultVersion());
        summary.setNotifyVersion(payload.notifyVersion());
        summary.setResultStatus(payload.resultStatus());
        summary.setPublicDate(payload.publicDate());
        summary.setDecisionNo(payload.decisionNo());
        summary.setDecisionDate(payload.decisionDate());
        summary.setDecisionAgency(payload.decisionAgency());
        summary.setDecisionFileId(payload.decisionFileId());
        summary.setDecisionFileName(payload.decisionFileName());
        summary.setEvalReportFileInfo(payload.evalReportFileInfo());
        summary.setHasWinner(payload.hasWinner());
        summary.setFetchedAt(OffsetDateTime.now());
        return summaryRepository.save(summary);
    }

    private Contractor upsertContractor(String contractorCode, String contractorName, String taxCode) {
        Contractor contractor = contractorRepository.findByContractorCode(contractorCode).orElse(null);
        if (contractor == null) {
            contractor = new Contractor();
            contractor.setContractorCode(contractorCode);
        }
        contractor.setContractorName(contractorName);
        if (!isBlank(taxCode)) {
            Set<String> taxCodes = new LinkedHashSet<>(contractor.getTaxCodes() == null ? List.of() : contractor.getTaxCodes());
            taxCodes.add(taxCode.trim());
            contractor.setTaxCodes(new ArrayList<>(taxCodes));
        }
        contractor.setFetchedAt(OffsetDateTime.now());
        return contractorRepository.save(contractor);
    }

    private boolean applyResultData(BiddingResult result, BiddingResultContractorPayload payload) {
        boolean changed = false;
        changed |= setIfChanged(result.getBidResult(), payload.bidResult(), result::setBidResult);
        changed |= setIfChanged(result.getWinningPrice(), payload.winningPrice(), result::setWinningPrice);
        changed |= setIfChanged(result.getReason(), payload.reason(), result::setReason);
        changed |= setIfChanged(result.getLotPrice(), payload.lotPrice(), result::setLotPrice);
        changed |= setIfChanged(result.getLotFinalPrice(), payload.lotFinalPrice(), result::setLotFinalPrice);
        changed |= setIfChanged(result.getAdjustedPrice(), payload.adjustedPrice(), result::setAdjustedPrice);
        changed |= setIfChanged(result.getEvalPrice(), payload.evalPrice(), result::setEvalPrice);
        changed |= setIfChanged(result.getTechScore(), payload.techScore(), result::setTechScore);
        changed |= setIfChanged(result.getDiscountRate(), payload.discountRate(), result::setDiscountRate);
        changed |= setIfChanged(result.getContractPeriod(), payload.contractPeriod(), result::setContractPeriod);
        changed |= setIfChanged(result.getContractPeriodUnit(), payload.contractPeriodUnit(), result::setContractPeriodUnit);
        changed |= setIfChanged(result.getContractPeriodText(), payload.contractPeriodText(), result::setContractPeriodText);
        changed |= setIfChanged(result.getContractExecutionTime(), payload.contractExecutionTime(), result::setContractExecutionTime);
        changed |= setIfChanged(result.getOtherContent(), payload.otherContent(), result::setOtherContent);
        return changed;
    }

    private void syncGoods(
            Contract contract,
            BiddingResultSummary summary,
            JsonNode main,
            Map<String, BiddingResult> resultByContractorCode
    ) {
        List<BiddingResultGoodsPayload> goodsItems = mapper.goods(main);
        biddingResultGoodsRepository.deleteByContract(contract);
        if (goodsItems.isEmpty()) {
            return;
        }

        OffsetDateTime fetchedAt = OffsetDateTime.now();
        List<BiddingResultGoods> goods = new ArrayList<>();
        for (BiddingResultGoodsPayload item : goodsItems) {
            String contractorCode = item.contractorCode();
            BiddingResult result = resultByContractorCode.get(normalizeCode(contractorCode));
            Contractor contractor = result == null || result.getBiddingContractor() == null
                    ? null
                    : result.getBiddingContractor().getContractor();

            BiddingResultGoods row = new BiddingResultGoods();
            row.setContract(contract);
            row.setResultSummary(summary);
            row.setBiddingResult(result);
            row.setContractor(contractor);
            row.setNotifyNo(item.notifyNo());
            row.setBidName(item.bidName());
            row.setLotNo(item.lotNo());
            row.setLotName(item.lotName());
            row.setContractorCode(contractorCode);
            row.setGoodsName(item.goodsName());
            row.setGoodsCode(item.goodsCode());
            row.setGoodsLabel(item.goodsLabel());
            row.setYearManufacture(item.yearManufacture());
            row.setOrigin(item.origin());
            row.setManufacturer(item.manufacturer());
            row.setTechnicalFeatures(item.technicalFeatures());
            row.setUnit(item.unit());
            row.setQuantity(item.quantity());
            row.setHsCode(item.hsCode());
            row.setWinningUnitPrice(item.winningUnitPrice());
            row.setAmount(item.amount());
            row.setDeliveryTime(item.deliveryTime());
            row.setSortOrder(item.sortOrder());
            row.setRawItem(item.rawItem());
            row.setFetchedAt(fetchedAt);
            goods.add(row);
        }
        biddingResultGoodsRepository.saveAll(goods);
    }

    private <T> boolean setIfChanged(T oldValue, T newValue, Consumer<T> setter) {
        if (Objects.equals(oldValue, newValue)) {
            return false;
        }
        setter.accept(newValue);
        return true;
    }

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim();
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.regionMatches(true, 0, "vn", 0, 2) ? normalized.substring(2).toLowerCase() : normalized.toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
