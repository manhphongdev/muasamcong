package com.muasamcong.service.biddingresult.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.biddingresult.BiddingResultSyncResult;
import com.muasamcong.integration.portal.PortalBiddingResultClient;
import com.muasamcong.integration.portal.PortalSearchClient;
import com.muasamcong.mapper.BiddingResultPayloadMapper;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.BiddingResult;
import com.muasamcong.model.BiddingResultSummary;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.model.Contractor;
import com.muasamcong.repository.BidOpeningRepository;
import com.muasamcong.repository.BiddingContractorRepository;
import com.muasamcong.repository.BiddingResultRepository;
import com.muasamcong.repository.BiddingResultSummaryRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.ContractorRepository;
import com.muasamcong.service.biddingresult.BiddingResultSyncService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final PortalSearchClient portalSearchClient;
    private final PortalBiddingResultClient portalBiddingResultClient;
    private final BiddingResultPayloadMapper mapper;
    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BidOpeningRepository bidOpeningRepository;
    private final ContractorRepository contractorRepository;
    private final BiddingContractorRepository biddingContractorRepository;
    private final BiddingResultRepository biddingResultRepository;
    private final BiddingResultSummaryRepository summaryRepository;

    @Override
    @Transactional
    public BiddingResultSyncResult syncByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        log.info("Sync bidding result start notifyNo={}", normalizedNotifyNo);

        ResolvedBidDetail resolved = portalSearchClient.resolve(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + normalizedNotifyNo));
        BidApiParams params = resolved.apiParams();
        if (isBlank(params.inputResultId())) {
            throw new IllegalStateException("Cannot resolve inputResultId: " + normalizedNotifyNo);
        }

        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));
        ContractInfo contractInfo = contractInfoRepository.findByContract(contract)
                .orElseThrow(() -> new IllegalArgumentException("ContractInfo not found: " + normalizedNotifyNo));
        BidOpening bidOpening = bidOpeningRepository.findByContractInfo(contractInfo)
                .orElseThrow(() -> new IllegalArgumentException("BidOpening not found: " + normalizedNotifyNo));

        JsonNode root = portalBiddingResultClient.fetchBiddingResult(params.inputResultId());
        JsonNode main = mapper.mainPayload(root);
        BiddingResultSummary summary = upsertSummary(contractInfo, main);

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;

        for (JsonNode item : mapper.contractorItems(main)) {
            String contractorCode = mapper.text(item, "orgCode");
            String contractorName = mapper.text(item, "orgFullname");
            if (isBlank(contractorCode) || isBlank(contractorName)) {
                skipped++;
                continue;
            }

            Contractor contractor = upsertContractor(contractorCode, contractorName, mapper.text(item, "taxCode"));
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

            boolean changed = applyResultData(result, item);
            if (newResult || changed) {
                result.setFetchedAt(OffsetDateTime.now());
                biddingResultRepository.save(result);
                if (newResult) {
                    created++;
                } else {
                    updated++;
                }
            } else {
                unchanged++;
            }
        }

        log.info("Sync bidding result done notifyNo={}, created={}, updated={}, unchanged={}, skipped={}",
                normalizedNotifyNo, created, updated, unchanged, skipped);
        return new BiddingResultSyncResult(normalizedNotifyNo, summary.getId(), created, updated, unchanged, skipped);
    }

    private BiddingResultSummary upsertSummary(ContractInfo contractInfo, JsonNode main) {
        BiddingResultSummary summary = summaryRepository.findByContractInfo(contractInfo).orElse(null);
        if (summary == null) {
            summary = new BiddingResultSummary();
            summary.setContractInfo(contractInfo);
        }

        summary.setResultVersion(mapper.text(main, "resultVersion"));
        summary.setNotifyVersion(mapper.text(main, "notifyVersion"));
        summary.setResultStatus(mapper.text(main, "status"));
        summary.setPublicDate(mapper.dateTime(main, "publicDate"));
        summary.setDecisionNo(mapper.text(main, "decisionNo"));
        summary.setDecisionDate(mapper.dateTime(main, "decisionDate"));
        summary.setDecisionAgency(mapper.text(main, "decisionAgency"));
        summary.setDecisionFileId(mapper.text(main, "decisionFileId"));
        summary.setDecisionFileName(mapper.text(main, "decisionFileName"));
        summary.setEvalReportFileInfo(mapper.text(main, "evalReportFileInfo"));
        summary.setHasWinner(mapper.hasWinner(main));
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

    private boolean applyResultData(BiddingResult result, JsonNode item) {
        boolean changed = false;
        changed |= setIfChanged(result.getBidResult(), mapper.integer(item, "bidResult"), result::setBidResult);
        changed |= setIfChanged(result.getWinningPrice(), mapper.longValue(item, "bidWiningPrice"), result::setWinningPrice);
        changed |= setIfChanged(result.getReason(), mapper.text(item, "reason"), result::setReason);
        changed |= setIfChanged(result.getLotPrice(), mapper.longValue(item, "lotPrice"), result::setLotPrice);
        changed |= setIfChanged(result.getLotFinalPrice(), mapper.longValue(item, "lotFinalPrice"), result::setLotFinalPrice);
        changed |= setIfChanged(result.getAdjustedPrice(), mapper.longValue(item, "adjustPrice"), result::setAdjustedPrice);
        changed |= setIfChanged(result.getEvalPrice(), mapper.longValue(item, "evalBidPrice"), result::setEvalPrice);
        changed |= setIfChanged(result.getTechScore(), mapper.decimal(item, "techScore"), result::setTechScore);
        changed |= setIfChanged(result.getDiscountRate(), mapper.decimal(item, "discountPercent"), result::setDiscountRate);
        changed |= setIfChanged(result.getContractPeriod(), mapper.integer(item, "cperiod"), result::setContractPeriod);
        changed |= setIfChanged(result.getContractPeriodUnit(), mapper.text(item, "cperiodUnit"), result::setContractPeriodUnit);
        changed |= setIfChanged(result.getContractPeriodText(), mapper.text(item, "cperiodText"), result::setContractPeriodText);
        changed |= setIfChanged(result.getContractExecutionTime(), mapper.text(item, "bidExecutionTime"), result::setContractExecutionTime);
        changed |= setIfChanged(result.getOtherContent(), mapper.text(item, "otherContent"), result::setOtherContent);
        return changed;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
