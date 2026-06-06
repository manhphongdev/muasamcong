package com.muasamcong.service.contractor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.muasamcong.dto.contractor.ContractorSyncResult;
import com.muasamcong.integration.portal.PortalContractorClient;
import com.muasamcong.mapper.ContractorPayloadMapper;
import com.muasamcong.model.Contractor;
import com.muasamcong.repository.ContractorRepository;
import com.muasamcong.service.contractor.ContractorSyncService;
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
public class ContractorSyncServiceImpl implements ContractorSyncService {
    private static final int DEFAULT_PAGE_SIZE = 500;
    private static final int MAX_PAGE_SIZE = 500;

    private final PortalContractorClient portalContractorClient;
    private final ContractorPayloadMapper mapper;
    private final ContractorRepository contractorRepository;

    @Override
    public ContractorSyncResult syncContractors(int pageSize) {
        int safePageSize = normalizePageSize(pageSize);
        log.info("Sync contractors start pageSize={}", safePageSize);

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        long totalElements = 0;
        int totalPages = 0;

        int pageNumber = 0;
        while (true) {
            JsonNode root = portalContractorClient.fetchContractors(pageNumber, safePageSize);
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

            log.info("Sync contractors page={} created={}, updated={}, unchanged={}, failed={}",
                    pageNumber, pageResult.created(), pageResult.updated(), pageResult.unchanged(), pageResult.failed());

            pageNumber++;
            if (pageNumber >= totalPages) {
                break;
            }
        }

        log.info("Sync contractors done created={}, updated={}, unchanged={}, failed={}",
                created, updated, unchanged, failed);

        return new ContractorSyncResult(totalElements, totalPages, created, updated, unchanged, failed);
    }

    private PageSyncResult syncPage(JsonNode content) {
        List<JsonNode> items = new ArrayList<>();
        Set<String> contractorCodes = new LinkedHashSet<>();
        int failed = 0;

        for (JsonNode item : content) {
            String contractorCode = mapper.text(item, "orgCode");
            if (contractorCode == null) {
                failed++;
                continue;
            }

            items.add(item);
            contractorCodes.add(contractorCode);
        }

        Map<String, Contractor> existingByCode = contractorRepository.findByContractorCodeIn(contractorCodes)
                .stream()
                .collect(Collectors.toMap(Contractor::getContractorCode, Function.identity()));

        List<Contractor> changedContractors = new ArrayList<>();
        Set<String> processedCodes = new LinkedHashSet<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (JsonNode item : items) {
            String contractorCode = mapper.text(item, "orgCode");
            String contractorName = mapper.text(item, "orgFullname");

            if (contractorCode == null || contractorName == null || !processedCodes.add(contractorCode)) {
                failed++;
                continue;
            }

            Contractor contractor = existingByCode.get(contractorCode);
            if (contractor == null) {
                contractor = new Contractor();
                contractor.setContractorCode(contractorCode);
                applyPortalData(contractor, item);
                contractor.setFetchedAt(OffsetDateTime.now());
                changedContractors.add(contractor);
                created++;
                continue;
            }

            if (applyPortalData(contractor, item)) {
                contractor.setFetchedAt(OffsetDateTime.now());
                changedContractors.add(contractor);
                updated++;
            } else {
                unchanged++;
            }
        }

        contractorRepository.saveAll(changedContractors);

        return new PageSyncResult(created, updated, unchanged, failed);
    }

    private boolean applyPortalData(Contractor contractor, JsonNode item) {
        boolean changed = false;

        changed |= setIfChanged(contractor.getContractorName(), mapper.text(item, "orgFullname"), contractor::setContractorName);
        changed |= setIfChanged(contractor.getAddress(), mapper.text(item, "officeAdd"), contractor::setAddress);
        changed |= setIfChanged(contractor.getOperatingStatus(), mapper.operatingStatus(item), contractor::setOperatingStatus);
        changed |= setIfChanged(contractor.getApprovedAt(), mapper.approvedAt(item), contractor::setApprovedAt);

        List<String> taxCodes = new ArrayList<>(mapper.taxCodes(item));
        if (!Objects.equals(contractor.getTaxCodes(), taxCodes)) {
            contractor.setTaxCodes(taxCodes);
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
