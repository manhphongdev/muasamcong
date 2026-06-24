package com.muasamcong.service.bidpackage.impl;

import com.muasamcong.dto.bidpackage.BidPackageTrackingDto;
import com.muasamcong.dto.bidpackage.BidderDto;
import com.muasamcong.dto.bidpackage.MissingFieldDto;
import com.muasamcong.dto.bidpackage.ScrapingLogDto;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.Bidding;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.BiddingDocument;
import com.muasamcong.model.BiddingResult;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.model.SyncItem;
import com.muasamcong.repository.BiddingContractorRepository;
import com.muasamcong.repository.BiddingDocumentRepository;
import com.muasamcong.repository.BiddingRepository;
import com.muasamcong.repository.BiddingResultRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.repository.SyncItemRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidPackageTrackingReader {
    private final SyncItemRepository syncItemRepository;
    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BiddingRepository biddingRepository;
    private final BiddingResultRepository biddingResultRepository;
    private final BiddingDocumentRepository biddingDocumentRepository;
    private final BiddingContractorRepository biddingContractorRepository;

    @Transactional(readOnly = true)
    public Page<BidPackageTrackingDto> searchTracking(String search, String status, String kpiFilter, Pageable pageable) {
        Page<SyncItem> itemPage = syncItemRepository.searchTracking(
                normalizeFilter(search),
                normalizeFilter(status),
                normalizeFilter(kpiFilter),
                OffsetDateTime.now().plusDays(1),
                pageable
        );
        if (itemPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, itemPage.getTotalElements());
        }

        List<String> notifyNos = itemPage.getContent().stream()
                .map(SyncItem::getNotifyNo)
                .toList();
        List<Contract> contracts = contractRepository.findByNotifyNoIn(notifyNos);
        Map<String, Contract> contractByNotifyNo = contracts.stream()
                .collect(Collectors.toMap(Contract::getNotifyNo, Function.identity(), (first, second) -> first));
        Map<Long, ContractInfo> infoByContractId = contracts.isEmpty() ? Map.of() : contractInfoRepository.findByContractInAndStatus(contracts, RecordStatus.ACTIVE).stream()
                .collect(Collectors.toMap(info -> info.getContract().getId(), Function.identity(), (first, second) -> first));
        Map<Long, Bidding> biddingByContractId = contracts.isEmpty() ? Map.of() : biddingRepository.findByContractIn(contracts).stream()
                .collect(Collectors.toMap(bidding -> bidding.getContract().getId(), Function.identity(), (first, second) -> first));
        Map<Long, List<BiddingContractor>> biddersByContractId = contracts.isEmpty() ? Map.of() : biddingContractorRepository.findByContracts(contracts).stream()
                .collect(Collectors.groupingBy(bidder -> bidder.getBidOpening().getContract().getId()));
        Map<Long, BiddingResult> winnerByContractId = contracts.isEmpty() ? Map.of() : biddingResultRepository.findWinnersByContracts(contracts).stream()
                .collect(Collectors.toMap(
                        result -> result.getBiddingContractor().getBidOpening().getContract().getId(),
                        Function.identity(),
                        (first, second) -> first
                ));
        Map<Long, List<BiddingDocument>> documentsByContractId = contracts.isEmpty() ? Map.of() : biddingDocumentRepository.findByContractIn(contracts).stream()
                .collect(Collectors.groupingBy(document -> document.getContract().getId()));

        List<BidPackageTrackingDto> content = itemPage.getContent().stream()
                .map(item -> toTrackingDto(
                        item,
                        contractByNotifyNo.get(item.getNotifyNo()),
                        infoByContractId,
                        biddingByContractId,
                        biddersByContractId,
                        winnerByContractId,
                        documentsByContractId
                ))
                .toList();

        return new PageImpl<>(content, pageable, itemPage.getTotalElements());
    }

    private BidPackageTrackingDto toTrackingDto(
            SyncItem item,
            Contract contract,
            Map<Long, ContractInfo> infoByContractId,
            Map<Long, Bidding> biddingByContractId,
            Map<Long, List<BiddingContractor>> biddersByContractId,
            Map<Long, BiddingResult> winnerByContractId,
            Map<Long, List<BiddingDocument>> documentsByContractId
    ) {
        ContractInfo info = contract == null ? null : infoByContractId.get(contract.getId());
        Bidding bidding = contract == null ? null : biddingByContractId.get(contract.getId());
        List<BiddingContractor> bidders = contract == null ? List.of() : biddersByContractId.getOrDefault(contract.getId(), List.of());
        BiddingResult winner = contract == null ? null : winnerByContractId.get(contract.getId());
        List<BiddingDocument> documents = contract == null ? List.of() : documentsByContractId.getOrDefault(contract.getId(), List.of());
        List<BidderDto> bidderDtos = toBidderDtos(bidders);

        return new BidPackageTrackingDto(
                item.getNotifyNo(),
                info == null || info.getBidName() == null ? item.getFolderName() : info.getBidName(),
                info == null || info.getInvestor() == null ? null : info.getInvestor().getInvestorName(),
                item.getFolderName(),
                item.getSourcePath(),
                folderExists(item.getSourcePath()),
                info == null ? null : firstNonNull(info.getBidEstimatePrice(), info.getBidPrice()),
                formatDate(item.getCreatedAt()),
                bidding == null ? null : bidding.getBidCloseAt(),
                bidding == null ? null : bidding.getBidOpenAt(),
                executionTime(info, winner),
                winner == null ? null : winner.getWinningPrice(),
                winningContractor(winner),
                contract == null ? null : contract.getBidUrl(),
                lifecycle(contract),
                item.getSyncStatus() == null ? null : item.getSyncStatus().name(),
                item.getLastSyncedAt(),
                item.getLastError(),
                completenessPercent(info, bidding, bidderDtos, winner),
                downloadCompletenessPercent(documents),
                documents.size(),
                downloadedDocumentCount(documents),
                failedDocumentCount(documents),
                pendingDocumentCount(documents),
                apiCoverage(info, bidding, bidderDtos, winner, documents),
                bidderDtos,
                logs(item),
                missingFields(info, bidding),
                item.getLastError() == null ? 0 : 1
        );
    }

    private List<BidderDto> toBidderDtos(List<BiddingContractor> bidders) {
        List<BiddingContractor> sorted = bidders.stream()
                .sorted(Comparator.comparing(BiddingContractor::getId))
                .toList();
        List<BidderDto> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            BiddingContractor bidder = sorted.get(i);
            result.add(new BidderDto(
                    i + 1,
                    firstTaxCode(bidder),
                    bidder.getContractor() == null ? null : bidder.getContractor().getContractorName(),
                    bidder.getBidPrice(),
                    bidder.getDiscountRate(),
                    bidder.getBidPriceAfterDiscount(),
                    bidder.getBidValidityPeriod(),
                    bidder.getBidGuaranteeValue() == null ? null : bidder.getBidGuaranteeValue() + " VND",
                    bidder.getBidGuaranteeValidityPeriod(),
                    bidder.getContractExecutionTime()
            ));
        }
        return result;
    }

    private String firstTaxCode(BiddingContractor bidder) {
        if (bidder.getContractor() == null || bidder.getContractor().getTaxCodes() == null || bidder.getContractor().getTaxCodes().isEmpty()) {
            return null;
        }
        return bidder.getContractor().getTaxCodes().get(0);
    }

    private String executionTime(ContractInfo info, BiddingResult winner) {
        if (winner != null) {
            if (winner.getContractExecutionTime() != null && !winner.getContractExecutionTime().isBlank()) {
                return winner.getContractExecutionTime();
            }
            if (winner.getContractPeriodText() != null && !winner.getContractPeriodText().isBlank()) {
                return winner.getContractPeriodText();
            }
            if (winner.getContractPeriod() != null) {
                return winner.getContractPeriod() + " " + Optional.ofNullable(winner.getContractPeriodUnit()).orElse("ngay");
            }
        }
        if (info != null && info.getContractPeriod() != null) {
            return info.getContractPeriod() + " " + Optional.ofNullable(info.getContractPeriodUnit()).orElse("ngay");
        }
        return null;
    }

    private String winningContractor(BiddingResult winner) {
        if (winner == null || winner.getBiddingContractor() == null || winner.getBiddingContractor().getContractor() == null) {
            return null;
        }
        return winner.getBiddingContractor().getContractor().getContractorName();
    }

    private String lifecycle(Contract contract) {
        if (contract == null || contract.getBidStatus() == null) {
            return "Không xác định";
        }
        return switch (contract.getBidStatus()) {
            case INVITATION_OPEN -> "Đang mời thầu";
            case BID_OPENED -> "Đã mở thầu";
            case BIDDING_CLOSED -> "Đã đóng thầu";
            case CONTRACTOR_SELECTION_RESULT_AVAILABLE -> "Có KQLCNT";
            case CONTRACT_INFORMATION_AVAILABLE -> "Có thông tin hợp đồng";
        };
    }

    private Integer completenessPercent(ContractInfo info, Bidding bidding, List<BidderDto> bidders, BiddingResult winner) {
        int total = 6;
        int done = 0;
        if (info != null && info.getBidName() != null) done++;
        if (info != null && (info.getBidEstimatePrice() != null || info.getBidPrice() != null)) done++;
        if (info != null && info.getInvestor() != null) done++;
        if (bidding != null && bidding.getBidCloseAt() != null) done++;
        if (bidding != null && bidding.getBidOpenAt() != null) done++;
        if (!bidders.isEmpty() || winner != null) done++;
        return done * 100 / total;
    }

    private Integer downloadCompletenessPercent(List<BiddingDocument> documents) {
        if (documents.isEmpty()) {
            return 0;
        }
        int completed = downloadedDocumentCount(documents);
        return completed * 100 / documents.size();
    }

    private Integer downloadedDocumentCount(List<BiddingDocument> documents) {
        return (int) documents.stream()
                .filter(document -> "DOWNLOADED".equalsIgnoreCase(document.getDownloadStatus()) || document.getDownloadedAt() != null)
                .count();
    }

    private Integer failedDocumentCount(List<BiddingDocument> documents) {
        return (int) documents.stream()
                .filter(document -> "FAILED".equalsIgnoreCase(document.getDownloadStatus()))
                .count();
    }

    private Integer pendingDocumentCount(List<BiddingDocument> documents) {
        int pending = documents.size() - downloadedDocumentCount(documents) - failedDocumentCount(documents);
        return Math.max(pending, 0);
    }

    private Map<String, String> apiCoverage(ContractInfo info, Bidding bidding, List<BidderDto> bidders, BiddingResult winner, List<BiddingDocument> documents) {
        Map<String, String> coverage = new LinkedHashMap<>();
        coverage.put("Search", info == null ? "Missing" : "Success");
        coverage.put("TBMT", info == null ? "Missing" : "Success");
        coverage.put("HSMT", documents.isEmpty() ? "Missing" : "Success");
        coverage.put("BaoCaoOpenDetail", bidders.isEmpty() ? "Missing" : "Success");
        coverage.put("KQLCNT", winner == null ? "Missing" : "Success");
        coverage.put("Contract", bidding == null ? "Missing" : "Success");
        return coverage;
    }

    private List<ScrapingLogDto> logs(SyncItem item) {
        if (item.getLastError() == null || item.getLastError().isBlank()) {
            return List.of();
        }
        return List.of(new ScrapingLogDto(
                formatDateTime(firstNonNull(item.getLastAttemptedAt(), item.getUpdatedAt())),
                "ERROR",
                "Sync",
                item.getLastError()
        ));
    }

    private List<MissingFieldDto> missingFields(ContractInfo info, Bidding bidding) {
        List<MissingFieldDto> missingFields = new ArrayList<>();
        if (info == null || info.getBidName() == null) {
            missingFields.add(new MissingFieldDto("title", "TBMT", "Chưa có tên gói thầu", "Chạy đồng bộ gói thầu"));
        }
        if (info == null || info.getInvestor() == null) {
            missingFields.add(new MissingFieldDto("investor", "TBMT", "Chưa có bên mời thầu", "Chạy đồng bộ gói thầu"));
        }
        if (bidding == null || bidding.getBidCloseAt() == null) {
            missingFields.add(new MissingFieldDto("closeTime", "TBMT", "Chưa có thời điểm đóng thầu", "Chạy đồng bộ TBMT"));
        }
        return missingFields;
    }

    private Boolean folderExists(String folderPath) {
        return folderPath != null && !folderPath.isBlank();
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim()) ? null : value.trim();
    }

    private String formatDate(OffsetDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
