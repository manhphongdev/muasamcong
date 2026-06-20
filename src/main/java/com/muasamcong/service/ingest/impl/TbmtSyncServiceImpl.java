package com.muasamcong.service.ingest.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.BidApiParams;
import com.muasamcong.dto.PortalSyncContext;
import com.muasamcong.dto.TbmtIngestResult;
import com.muasamcong.dto.TbmtPayload;
import com.muasamcong.enums.RecordStatus;
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
    private final PortalTbmt portalTbmtClient;
    private final TbmtPayloadMapper mapper;

    @Override
    @Transactional
    public TbmtIngestResult sync(PortalSyncContext context) {
        String normalizedNotifyNo = normalizeNotifyNo(context.notifyNo());
        log.info("Sync TBMT start notifyNo={}", normalizedNotifyNo);

        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));

        BidApiParams params = context.apiParams();
        JsonNode root = portalTbmtClient.fetchTbmt(params.notifyId());
        TbmtPayload payload = mapper.toPayload(root);

        Investor investor = upsertInvestor(payload);
        upsertProcurementPlan(contract, payload, investor);
        contract.setBidUrl(context.detailUrl());
        UpsertedContractInfo upsertedContractInfo = upsertContractInfo(contract, payload, investor);
        Bidding bidding = upsertBidding(contract, payload);
        upsertBidOpening(contract, payload);

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

    private UpsertedContractInfo upsertContractInfo(Contract contract, TbmtPayload payload, Investor investor) {
        String version = normalizeVersion(payload.notifyVersion());
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
        info.setBusinessStatus(payload.businessStatus());
        info.setVersion(version);
        info.setBidName(payload.bidName());
        info.setInvestor(investor);
        info.setCapitalDetail(payload.capitalDetail());
        info.setInvestField(payload.investField());
        info.setBidForm(payload.bidForm());
        info.setContractType(payload.contractType());
        info.setBidMode(payload.bidMode());
        info.setContractPeriod(payload.contractPeriod());
        info.setContractPeriodUnit(payload.contractPeriodUnit());
        info.setMultiLot(payload.multiLot());
        info.setDomestic(payload.domestic());
        info.setBidPrice(payload.bidPrice());
        info.setBidPriceUnit(payload.bidPriceUnit());
        info.setBidEstimatePrice(payload.bidEstimatePrice());
        info.setBidValidityPeriod(payload.bidValidityPeriod());
        info.setBidValidityPeriodUnit(payload.bidValidityPeriodUnit());
        info.setPrequalification(payload.prequalification());
        info.setFetchedAt(OffsetDateTime.now());

        return new UpsertedContractInfo(contractInfoRepository.save(info), created);
    }

    private Bidding upsertBidding(Contract contract, TbmtPayload payload) {
        Bidding bidding = biddingRepository.findByContract(contract).orElse(null);
        if (bidding == null) {
            bidding = new Bidding();
            bidding.setContract(contract);
        }

        bidding.setInternet(payload.internet());
        bidding.setSubmissionMethod(payload.submissionMethod());
        bidding.setIssueLocation(payload.issueLocation());
        bidding.setReceiveLocation(payload.receiveLocation());
        bidding.setExecutionLocation(payload.executionLocation());
        bidding.setFeeType(payload.feeType());
        bidding.setFeeValue(payload.feeValue());
        bidding.setFeeUnit(payload.feeUnit());
        bidding.setBidCloseAt(payload.bidCloseAt());
        bidding.setBidOpenAt(payload.bidOpenAt());
        bidding.setBidOpenLocation(payload.bidOpenLocation());
        bidding.setGuaranteeValue(payload.guaranteeValue());
        bidding.setGuaranteeUnit(payload.guaranteeUnit());
        bidding.setGuaranteeForm(payload.guaranteeForm());
        bidding.setFetchedAt(OffsetDateTime.now());

        return biddingRepository.save(bidding);
    }

    private BidOpening upsertBidOpening(Contract contract, TbmtPayload payload) {
        OffsetDateTime completedAt = payload.bidOpeningCompletedAt();
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

    private Investor upsertInvestor(TbmtPayload payload) {
        String investorCode = payload.investorCode();
        String investorName = payload.investorName();

        if (investorCode == null || investorCode.isBlank()) {
            return null;
        }

        Investor investor = investorRepository.findByInvestorCode(investorCode).orElse(null);
        if (investor == null) {
            investor = new Investor();
        }

        investor.setInvestorCode(investorCode);
        investor.setInvestorName(investorName == null || investorName.isBlank() ? investorCode : investorName);
        investor.setOldInvestorName(payload.oldInvestorName());
        investor.setMergeInvestorDate(payload.mergeInvestorDate());
        investor.setFetchedAt(OffsetDateTime.now());

        return investorRepository.save(investor);
    }

    private void upsertProcurementPlan(Contract contract, TbmtPayload payload, Investor investor) {
        ProcurementPlan procurementPlan = contract.getProcurementPlan();
        if (procurementPlan == null) {
            String planNo = payload.planNo();
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

        String planName = payload.planName();
        if (planName != null && !planName.isBlank()) {
            procurementPlan.setPlanName(planName);
        }
        if (investor != null) {
            procurementPlan.setInvestor(investor);
        }
        procurementPlan.setFetchedAt(OffsetDateTime.now());
        procurementPlanRepository.save(procurementPlan);
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
