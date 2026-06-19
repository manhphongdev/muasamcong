package com.muasamcong.service.ingest.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.integration.portal.PortalSearch;
import com.muasamcong.integration.portal.PortalTbmt;
import com.muasamcong.mapper.TbmtPayloadMapper;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.Bidding;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.model.Investor;
import com.muasamcong.model.ProcurementPlan;
import com.muasamcong.repository.BidOpeningRepository;
import com.muasamcong.repository.BiddingRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.InvestorRepository;
import com.muasamcong.repository.ProcurementPlanRepository;
import java.time.OffsetDateTime;
import com.muasamcong.service.ingest.TbmtSyncService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TbmtSyncServiceImpl implements TbmtSyncService {
    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BidOpeningRepository bidOpeningRepository;
    private final BiddingRepository biddingRepository;
    private final InvestorRepository investorRepository;
    private final ProcurementPlanRepository procurementPlanRepository;
    private final PortalSearch portalSearchClient;
    private final PortalTbmt portalTbmtClient;
    private final TbmtPayloadMapper mapper;

    @Override
    @Transactional
    public TbmtIngestResult syncByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        log.info("Sync TBMT start notifyNo={}", normalizedNotifyNo);

        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));

        ResolvedBidDetail resolved = portalSearchClient.resolve(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + normalizedNotifyNo));

        BidApiParams params = resolved.apiParams();
        JsonNode root = portalTbmtClient.fetchTbmt(params.notifyId());
        JsonNode tbmt = mapper.mainPayload(root);

        Investor investor = upsertInvestor(tbmt);
        upsertProcurementPlan(contract, tbmt, investor);
        contract.setBidUrl(resolved.detailUrl());
        UpsertedContractInfo upsertedContractInfo = upsertContractInfo(contract, tbmt, investor);
        Bidding bidding = upsertBidding(contract, tbmt);
        upsertBidOpening(contract, root);

        log.info("Sync TBMT done notifyNo={}, created={}", normalizedNotifyNo, upsertedContractInfo.created());
        return new TbmtIngestResult(
                normalizedNotifyNo,
                contract.getId(),
                upsertedContractInfo.contractInfo().getId(),
                bidding.getId(),
                upsertedContractInfo.created(),
                bidding.getBidCloseAt(),
                bidding.getBidOpenAt()
        );
    }

    private UpsertedContractInfo upsertContractInfo(Contract contract, JsonNode tbmt, Investor investor) {
        String version = normalizeVersion(mapper.text(tbmt, "notifyVersion"));
        ContractInfo activeInfo = contractInfoRepository.findByContractAndStatus(contract, RecordStatus.ACTIVE).orElse(null);
        boolean created = activeInfo == null;
        ContractInfo info = activeInfo;

        if (activeInfo != null && !Objects.equals(activeInfo.getVersion(), version)) {
            activeInfo.setStatus(RecordStatus.INACTIVE);
            contractInfoRepository.save(activeInfo);
            info = contractInfoRepository.findByContractAndVersion(contract, version).orElse(null);
            created = info == null;
        }

        if (info == null) {
            info = new ContractInfo();
            info.setContract(contract);
        }

        info.setStatus(RecordStatus.ACTIVE);
        info.setBusinessStatus(mapper.text(tbmt, "status"));
        info.setVersion(version);
        info.setBidName(mapper.text(tbmt, "bidName"));
        info.setInvestor(investor);
        info.setCapitalDetail(mapper.text(tbmt, "capitalDetail"));
        info.setInvestField(mapper.text(tbmt, "investField"));
        info.setBidForm(mapper.text(tbmt, "bidForm"));
        info.setContractType(mapper.text(tbmt, "contractType"));
        info.setBidMode(mapper.text(tbmt, "bidMode"));
        info.setContractPeriod(mapper.integer(tbmt, "contractPeriod"));
        info.setContractPeriodUnit(mapper.text(tbmt, "contractPeriodUnit"));
        info.setMultiLot(resolveMultiLot(tbmt));
        info.setDomestic(mapper.booleanValue(tbmt, "isDomestic"));
        info.setBidPrice(mapper.longValue(tbmt, "bidPrice"));
        info.setBidPriceUnit(mapper.text(tbmt, "bidPriceUnit"));
        info.setBidEstimatePrice(mapper.longValue(tbmt, "bidEstimatePrice"));
        info.setBidValidityPeriod(mapper.integer(tbmt, "bidValidityPeriod"));
        info.setBidValidityPeriodUnit(mapper.text(tbmt, "bidValidityPeriodUnit"));
        info.setPrequalification(mapper.booleanValue(tbmt, "isPrequalification"));
        info.setFetchedAt(OffsetDateTime.now());

        return new UpsertedContractInfo(contractInfoRepository.save(info), created);
    }

    private Bidding upsertBidding(Contract contract, JsonNode tbmt) {
        Bidding bidding = biddingRepository.findByContract(contract).orElse(null);
        if (bidding == null) {
            bidding = new Bidding();
            bidding.setContract(contract);
        }

        bidding.setInternet(mapper.booleanValue(tbmt, "isInternet"));
        bidding.setSubmissionMethod(mapper.text(tbmt, "submissionMethod"));
        bidding.setIssueLocation(mapper.text(tbmt, "issueLocation"));
        bidding.setReceiveLocation(mapper.text(tbmt, "receiveLocation"));
        bidding.setExecutionLocation(mapper.text(tbmt, "executionLocation"));
        bidding.setFeeType(mapper.text(tbmt, "feeType"));
        bidding.setFeeValue(mapper.decimal(tbmt, "feeValue"));
        bidding.setFeeUnit(mapper.text(tbmt, "feeUnit"));
        bidding.setBidCloseAt(mapper.dateTime(tbmt, "bidCloseDate"));
        bidding.setBidOpenAt(mapper.dateTime(tbmt, "bidOpenDate"));
        bidding.setBidOpenLocation(mapper.text(tbmt, "bidOpenLocation"));
        bidding.setGuaranteeValue(mapper.decimal(tbmt, "guaranteeValue"));
        bidding.setGuaranteeUnit(mapper.text(tbmt, "guaranteeUnit"));
        bidding.setGuaranteeForm(mapper.text(tbmt, "guaranteeForm"));
        bidding.setFetchedAt(OffsetDateTime.now());

        return biddingRepository.save(bidding);
    }

    private BidOpening upsertBidOpening(Contract contract, JsonNode root) {
        JsonNode bidStatus = root == null ? null : root.get("bidoBidStatus");
        OffsetDateTime completedAt = mapper.dateTime(bidStatus, "successBidOpenDate");
        if (completedAt == null) {
            return null;
        }

        BidOpening bidOpening = bidOpeningRepository.findByContract(contract).orElse(null);
        if (bidOpening == null) {
            bidOpening = new BidOpening();
            bidOpening.setContract(contract);
        }

        bidOpening.setCompletedAt(completedAt);
        return bidOpeningRepository.save(bidOpening);
    }

    private Investor upsertInvestor(JsonNode tbmt) {
        String investorCode = mapper.text(tbmt, "investorCode");
        String investorName = mapper.text(tbmt, "investorName");

        if (investorCode == null || investorCode.isBlank()) {
            return null;
        }

        Investor investor = investorRepository.findByInvestorCode(investorCode).orElse(null);
        if (investor == null) {
            investor = new Investor();
        }

        investor.setInvestorCode(investorCode);
        investor.setInvestorName(investorName == null || investorName.isBlank() ? investorCode : investorName);
        investor.setOldInvestorName(mapper.text(tbmt, "oldInvestorName"));
        investor.setMergeInvestorDate(mapper.dateTime(tbmt, "mergeInvestorDate"));
        investor.setFetchedAt(OffsetDateTime.now());

        return investorRepository.save(investor);
    }

    private void upsertProcurementPlan(Contract contract, JsonNode tbmt, Investor investor) {
        ProcurementPlan procurementPlan = contract.getProcurementPlan();
        if (procurementPlan == null) {
            String planNo = mapper.text(tbmt, "planNo");
            if (planNo == null || planNo.isBlank()) {
                return;
            }
            procurementPlan = procurementPlanRepository.findByPlanNo(planNo).orElseGet(() -> {
                ProcurementPlan newPlan = new ProcurementPlan();
                newPlan.setPlanNo(planNo);
                return newPlan;
            });
            contract.setProcurementPlan(procurementPlan);
        }

        String planName = mapper.text(tbmt, "planName");
        if (planName != null && !planName.isBlank()) {
            procurementPlan.setPlanName(planName);
        }
        if (investor != null) {
            procurementPlan.setInvestor(investor);
        }
        procurementPlan.setFetchedAt(OffsetDateTime.now());
        procurementPlanRepository.save(procurementPlan);
    }

    private Boolean resolveMultiLot(JsonNode tbmt) {
        Boolean value = mapper.booleanValue(tbmt, "isMultiLot");
        if (value != null) {
            return value;
        }
        JsonNode lotList = tbmt == null ? null : tbmt.get("lotDTOList");
        return lotList != null && lotList.isArray() && lotList.size() > 1;
    }

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim();
    }

    private String normalizeVersion(String version) {
        return version == null || version.isBlank() ? "0" : version.trim();
    }

    private record UpsertedContractInfo(ContractInfo contractInfo, boolean created) {
    }
}
