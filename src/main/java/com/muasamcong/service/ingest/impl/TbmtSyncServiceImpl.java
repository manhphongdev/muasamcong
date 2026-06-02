package com.muasamcong.service.ingest.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.ResolvedBidDetail;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.integration.portal.PortalSearchClient;
import com.muasamcong.integration.portal.PortalTbmtClient;
import com.muasamcong.mapper.TbmtPayloadMapper;
import com.muasamcong.model.Bidding;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.model.Investor;
import com.muasamcong.repository.BiddingRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.InvestorRepository;
import java.time.OffsetDateTime;
import com.muasamcong.service.ingest.TbmtSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TbmtSyncServiceImpl implements TbmtSyncService {
    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BiddingRepository biddingRepository;
    private final InvestorRepository investorRepository;
    private final PortalSearchClient portalSearchClient;
    private final PortalTbmtClient portalTbmtClient;
    private final TbmtPayloadMapper mapper;

    @Override
    @Transactional
    public TbmtIngestResult syncByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);

        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));

        ResolvedBidDetail resolved = portalSearchClient.resolve(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve notifyNo: " + normalizedNotifyNo));

        BidApiParams params = resolved.apiParams();
        JsonNode root = portalTbmtClient.fetchTbmt(params.notifyId());
        JsonNode tbmt = mapper.mainPayload(root);

        Investor investor = upsertInvestor(tbmt);
        UpsertedContractInfo upsertedContractInfo = upsertContractInfo(contract, tbmt, investor);
        Bidding bidding = upsertBidding(upsertedContractInfo.contractInfo(), tbmt);

        return new TbmtIngestResult(
                normalizedNotifyNo,
                contract.getId(),
                upsertedContractInfo.contractInfo().getId(),
                bidding.getId(),
                upsertedContractInfo.created()
        );
    }

    private UpsertedContractInfo upsertContractInfo(Contract contract, JsonNode tbmt, Investor investor) {
        ContractInfo info = contractInfoRepository.findByContract(contract).orElse(null);
        boolean created = info == null;
        if (created) {
            info = new ContractInfo();
            info.setContract(contract);
        }

        info.setBusinessStatus(mapper.text(tbmt, "status"));
        info.setLatestVersionNumber(mapper.text(tbmt, "notifyVersion"));
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
        info.setDomestic(mapper.boolFromInt(tbmt, "isDomestic"));
        info.setPrequalification(mapper.boolFromInt(tbmt, "isPrequalification"));
        info.setFetchedAt(OffsetDateTime.now());

        return new UpsertedContractInfo(contractInfoRepository.save(info), created);
    }

    private Bidding upsertBidding(ContractInfo contractInfo, JsonNode tbmt) {
        Bidding bidding = biddingRepository.findByContractInfo(contractInfo).orElse(null);
        if (bidding == null) {
            bidding = new Bidding();
            bidding.setContractInfo(contractInfo);
        }

        bidding.setInternet(mapper.boolFromInt(tbmt, "isInternet"));
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
        bidding.setBidValidityPeriod(mapper.integer(tbmt, "bidValidityPeriod"));
        bidding.setBidValidityUnit(mapper.text(tbmt, "bidValidityPeriodUnit"));
        bidding.setGuaranteeValue(mapper.decimal(tbmt, "guaranteeValue"));
        bidding.setGuaranteeUnit(mapper.text(tbmt, "guaranteeUnit"));
        bidding.setGuaranteeForm(mapper.text(tbmt, "guaranteeForm"));
        bidding.setFetchedAt(OffsetDateTime.now());

        return biddingRepository.save(bidding);
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

    private Boolean resolveMultiLot(JsonNode tbmt) {
        Boolean value = mapper.boolFromInt(tbmt, "isMultiLot");
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

    private record UpsertedContractInfo(ContractInfo contractInfo, boolean created) {
    }
}
