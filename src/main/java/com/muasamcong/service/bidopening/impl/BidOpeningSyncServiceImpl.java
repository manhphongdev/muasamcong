package com.muasamcong.service.bidopening.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.bidopening.BidOpeningSyncResult;
import com.muasamcong.integration.portal.PortalBidOpeningClient;
import com.muasamcong.integration.portal.PortalSearchClient;
import com.muasamcong.mapper.BidOpeningPayloadMapper;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.model.Contractor;
import com.muasamcong.repository.BidOpeningRepository;
import com.muasamcong.repository.BiddingContractorRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.ContractorRepository;
import com.muasamcong.service.bidopening.BidOpeningSyncService;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidOpeningSyncServiceImpl implements BidOpeningSyncService {
    private final PortalSearchClient portalSearchClient;
    private final PortalBidOpeningClient portalBidOpeningClient;
    private final BidOpeningPayloadMapper mapper;
    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BidOpeningRepository bidOpeningRepository;
    private final ContractorRepository contractorRepository;
    private final BiddingContractorRepository biddingContractorRepository;

    @Override
    @Transactional
    public BidOpeningSyncResult syncByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        log.info("Sync bid opening start notifyNo={}", normalizedNotifyNo);

        ResolvedBidDetail resolved = portalSearchClient.resolve(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + normalizedNotifyNo));
        BidApiParams params = resolved.apiParams();
        if (isBlank(params.notifyId()) || isBlank(params.bidOpenId()) && isBlank(params.bidPreOpenId())) {
            throw new IllegalStateException("Cannot resolve bid opening params: " + normalizedNotifyNo);
        }

        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));
        ContractInfo contractInfo = contractInfoRepository.findByContract(contract)
                .orElseThrow(() -> new IllegalArgumentException("ContractInfo not found: " + normalizedNotifyNo));

        JsonNode root = portalBidOpeningClient.fetchLotOpenDetail(params);
        BidOpening bidOpening = upsertBidOpening(contractInfo);

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;

        for (JsonNode item : mapper.contractorItems(root)) {
            String contractorCode = mapper.text(item, "contractorCode");
            String contractorName = mapper.text(item, "contractorName");
            if (isBlank(contractorCode) || isBlank(contractorName)) {
                skipped++;
                continue;
            }

            UpsertedContractor upsertedContractor = upsertContractor(contractorCode, contractorName);
            BiddingContractor biddingContractor = biddingContractorRepository
                    .findByBidOpeningAndContractor(bidOpening, upsertedContractor.contractor())
                    .orElse(null);
            boolean newBiddingContractor = biddingContractor == null;
            if (newBiddingContractor) {
                biddingContractor = new BiddingContractor();
                biddingContractor.setBidOpening(bidOpening);
                biddingContractor.setContractor(upsertedContractor.contractor());
            }

            boolean changed = applyBidOpeningData(biddingContractor, item, contractInfo);
            if (newBiddingContractor || changed) {
                biddingContractorRepository.save(biddingContractor);
                if (newBiddingContractor) {
                    created++;
                } else {
                    updated++;
                }
            } else {
                unchanged++;
            }
        }

        log.info("Sync bid opening done notifyNo={}, created={}, updated={}, unchanged={}, skipped={}",
                normalizedNotifyNo, created, updated, unchanged, skipped);
        return new BidOpeningSyncResult(normalizedNotifyNo, bidOpening.getId(), created, updated, unchanged, skipped);
    }

    private BidOpening upsertBidOpening(ContractInfo contractInfo) {
        BidOpening bidOpening = bidOpeningRepository.findByContractInfo(contractInfo).orElse(null);
        if (bidOpening == null) {
            bidOpening = new BidOpening();
            bidOpening.setContractInfo(contractInfo);
        }
        bidOpening.setFetchedAt(OffsetDateTime.now());
        return bidOpeningRepository.save(bidOpening);
    }

    private UpsertedContractor upsertContractor(String contractorCode, String contractorName) {
        Contractor contractor = contractorRepository.findByContractorCode(contractorCode).orElse(null);
        boolean created = contractor == null;
        if (created) {
            contractor = new Contractor();
            contractor.setContractorCode(contractorCode);
        }
        contractor.setContractorName(contractorName);
        contractor.setFetchedAt(OffsetDateTime.now());
        return new UpsertedContractor(contractorRepository.save(contractor), created);
    }

    private boolean applyBidOpeningData(BiddingContractor biddingContractor, JsonNode item, ContractInfo contractInfo) {
        boolean changed = false;
        changed |= setIfChanged(biddingContractor.getBidPrice(), mapper.longValue(item, "lotPrice"), biddingContractor::setBidPrice);
        changed |= setIfChanged(biddingContractor.getDiscountRate(), mapper.decimal(item, "discountPercent"), biddingContractor::setDiscountRate);
        changed |= setIfChanged(biddingContractor.getBidPriceAfterDiscount(), mapper.longValue(item, "lotFinalPrice"), biddingContractor::setBidPriceAfterDiscount);
        changed |= setIfChanged(biddingContractor.getBidValidityPeriod(), contractInfo.getBidValidityPeriod(), biddingContractor::setBidValidityPeriod);
        changed |= setIfChanged(biddingContractor.getBidGuaranteeValue(), mapper.longValue(item, "bidGuaranteeAmount"), biddingContractor::setBidGuaranteeValue);
        changed |= setIfChanged(biddingContractor.getBidGuaranteeValidityPeriod(), mapper.integer(item, "bidGuaranteeEff"), biddingContractor::setBidGuaranteeValidityPeriod);
        changed |= setIfChanged(biddingContractor.getContractExecutionTime(), mapper.text(item, "cperiodText"), biddingContractor::setContractExecutionTime);
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

    private record UpsertedContractor(Contractor contractor, boolean created) {
    }
}
