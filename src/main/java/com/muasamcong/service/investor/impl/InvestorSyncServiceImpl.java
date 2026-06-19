package com.muasamcong.service.investor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.investor.InvestorSyncResult;
import com.muasamcong.integration.portal.PortalInvestor;
import com.muasamcong.mapper.InvestorPayloadMapper;
import com.muasamcong.model.Investor;
import com.muasamcong.repository.InvestorRepository;
import com.muasamcong.service.investor.InvestorSyncService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorSyncServiceImpl implements InvestorSyncService {
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 500;

    private final PortalInvestor portalInvestorClient;
    private final InvestorPayloadMapper mapper;
    private final InvestorRepository investorRepository;

    @Override
    public InvestorSyncResult syncInvestors(int pageSize) {
        int safePageSize = normalizePageSize(pageSize);
        log.info("Sync investors start pageSize={}", safePageSize);

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        long totalElements = 0;
        int totalPages = 0;

        int pageNumber = 0;
        while (true) {
            JsonNode root = portalInvestorClient.fetchInvestors(pageNumber, safePageSize);
            totalElements = mapper.totalElements(root);
            totalPages = mapper.totalPages(root);

            JsonNode content = mapper.content(root);
            if (content == null || content.isEmpty()) {
                break;
            }

            PageSyncResult pageResult = syncPage(content);
            created += pageResult.created();
            updated += pageResult.updated();
            unchanged += pageResult.unchanged();
            failed += pageResult.failed();

            pageNumber++;
            if (pageNumber >= totalPages) {
                break;
            }
        }

        log.info("Sync investors done created={}, updated={}, unchanged={}, failed={}",
                created, updated, unchanged, failed);

        return new InvestorSyncResult(totalElements, totalPages, created, updated, unchanged, failed);
    }

    private PageSyncResult syncPage(JsonNode content) {
        List<JsonNode> items = new ArrayList<>();
        Set<String> investorCodes = new LinkedHashSet<>();
        int failed = 0;

        for (JsonNode item : content) {
            String investorCode = mapper.text(item, "orgCode");
            if (investorCode == null) {
                failed++;
                continue;
            }

            items.add(item);
            investorCodes.add(investorCode);
        }

        Map<String, Investor> existingByCode = investorRepository.findByInvestorCodeIn(investorCodes)
                .stream()
                .collect(Collectors.toMap(Investor::getInvestorCode, Function.identity()));

        List<Investor> changedInvestors = new ArrayList<>();
        Set<String> processedCodes = new LinkedHashSet<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (JsonNode item : items) {
            String investorCode = mapper.text(item, "orgCode");
            String investorName = mapper.text(item, "orgFullname");

            if (investorCode == null || investorName == null || !processedCodes.add(investorCode)) {
                failed++;
                continue;
            }

            Investor investor = existingByCode.get(investorCode);
            if (investor == null) {
                investor = new Investor();
                investor.setInvestorCode(investorCode);
                applyPortalData(investor, item);
                investor.setFetchedAt(OffsetDateTime.now());
                changedInvestors.add(investor);
                created++;
                continue;
            }

            if (applyPortalData(investor, item)) {
                investor.setFetchedAt(OffsetDateTime.now());
                changedInvestors.add(investor);
                updated++;
            } else {
                unchanged++;
            }
        }

        investorRepository.saveAll(changedInvestors);

        return new PageSyncResult(created, updated, unchanged, failed);
    }

    private boolean applyPortalData(Investor investor, JsonNode item) {
        boolean changed = false;

        changed |= setIfChanged(investor.getInvestorName(), mapper.text(item, "orgFullname"), investor::setInvestorName);
        changed |= setIfChanged(investor.getAddress(), mapper.text(item, "officeAdd"), investor::setAddress);
        changed |= setIfChanged(investor.getOperatingStatus(), mapper.operatingStatus(item), investor::setOperatingStatus);
        changed |= setIfChanged(investor.getApprovedAt(), mapper.approvedAt(item), investor::setApprovedAt);

        List<String> taxCodes = new ArrayList<>(mapper.taxCodes(item));
        if (!Objects.equals(investor.getTaxCodes(), taxCodes)) {
            investor.setTaxCodes(taxCodes);
            changed = true;
        }

        return changed;
    }

    private <T> boolean setIfChanged(T oldValue, T newValue, Consumer<T> setter) {
        if (Objects.equals(oldValue, newValue)) {
            return false;
        }

        setter.accept(newValue);
        return true;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private record PageSyncResult(int created, int updated, int unchanged, int failed) {
    }
}
