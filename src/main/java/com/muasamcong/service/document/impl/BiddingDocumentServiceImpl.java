package com.muasamcong.service.document.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muasamcong.dto.document.ClarificationContentResult;
import com.muasamcong.dto.document.ClarificationResult;
import com.muasamcong.dto.document.DocumentFileResult;
import com.muasamcong.dto.document.DocumentDownloadPendingResult;
import com.muasamcong.dto.document.DocumentEnqueueStats;
import com.muasamcong.dto.document.DocumentSummaryResult;
import com.muasamcong.dto.document.PetitionContentResult;
import com.muasamcong.dto.document.PetitionResult;
import com.muasamcong.dto.file.FileDownloadRequest;
import com.muasamcong.dto.file.FileDownloadResult;
import com.muasamcong.model.BiddingDocument;
import com.muasamcong.model.BidClarification;
import com.muasamcong.model.BidClarificationContent;
import com.muasamcong.model.BidPetition;
import com.muasamcong.model.BidPetitionContent;
import com.muasamcong.model.Contract;
import com.muasamcong.repository.BidClarificationContentRepository;
import com.muasamcong.repository.BidClarificationRepository;
import com.muasamcong.repository.BidPackageSyncItemRepository;
import com.muasamcong.repository.BidPetitionContentRepository;
import com.muasamcong.repository.BidPetitionRepository;
import com.muasamcong.repository.BiddingDocumentRepository;
import com.muasamcong.service.document.BiddingDocumentService;
import com.muasamcong.service.file.FileService;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingDocumentServiceImpl implements BiddingDocumentService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String PENDING = "PENDING";
    private static final String DOWNLOADING = "DOWNLOADING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private static final String KQLCNT_DECISION = "KQLCNT_DECISION";
    private static final String E_HSDT_EVAL_REPORT = "E_HSDT_EVAL_REPORT";
    private static final String HSMT_CLARIFICATION_REQUEST = "HSMT_CLARIFICATION_REQUEST";
    private static final String HSMT_CLARIFICATION_RESPONSE = "HSMT_CLARIFICATION_RESPONSE";
    private static final String PETITION_REQUEST = "PETITION_REQUEST";
    private static final String PETITION_RESPONSE = "PETITION_RESPONSE";
    private static final String PETITION_CANCEL = "PETITION_CANCEL";

    private static final String SOURCE_KQLCNT = "KQLCNT";
    private static final String SOURCE_CLARIFICATION = "CLARIFICATION";
    private static final String SOURCE_PETITION = "PETITION";
    private static final String ROLE_REQUEST = "REQUEST";
    private static final String ROLE_RESPONSE = "RESPONSE";
    private static final String ROLE_CANCEL = "CANCEL";
    private static final String AUTO_DOWNLOAD_FOLDER = "auto-download";

    private final BiddingDocumentRepository repository;
    private final BidPackageSyncItemRepository syncItemRepository;
    private final BidClarificationRepository clarificationRepository;
    private final BidClarificationContentRepository clarificationContentRepository;
    private final BidPetitionRepository petitionRepository;
    private final BidPetitionContentRepository petitionContentRepository;
    private final FileService fileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public DocumentEnqueueStats enqueueBiddingResultFiles(Contract contract, JsonNode main) {
        DocumentEnqueueStats stats = DocumentEnqueueStats.empty();
        stats = stats.plus(enqueue(contract, text(main, "decisionFileId"), text(main, "decisionFileName"), KQLCNT_DECISION, SOURCE_KQLCNT, null, null));
        stats = stats.plus(enqueueEvalReportFiles(contract, text(main, "evalReportFileInfo")));
        return stats;
    }

    @Override
    @Transactional
    public DocumentEnqueueStats enqueueClarificationFiles(Contract contract, JsonNode root) {
        DocumentEnqueueStats stats = DocumentEnqueueStats.empty();
        JsonNode versions = root == null ? null : root.get("biduClarifyReqInvAndContentViewVersionDTOList");
        if (versions == null || !versions.isArray()) {
            return stats;
        }

        for (JsonNode version : versions) {
            JsonNode items = version.get("biduClarifyReqInvAndContentViewList");
            if (items == null || !items.isArray()) {
                continue;
            }
            for (JsonNode item : items) {
                String reqNo = text(item, "reqNo");
                BidClarification clarification = upsertClarification(contract, text(version, "notifyVersion"), item);
                upsertClarificationContents(clarification, text(item, "clarifyReqContent"), text(item, "clarifyResContent"));
                stats = stats.plus(enqueue(contract, text(item, "clarify_file_id"), fallback(text(item, "clarify_file_name"), "clarification-request-" + reqNo + ".pdf"), HSMT_CLARIFICATION_REQUEST, SOURCE_CLARIFICATION, reqNo, ROLE_REQUEST));
                stats = stats.plus(enqueue(contract, text(item, "clarifyResFileId"), fallback(text(item, "clarifyResFileName"), "clarification-response-" + reqNo + ".pdf"), HSMT_CLARIFICATION_RESPONSE, SOURCE_CLARIFICATION, reqNo, ROLE_RESPONSE));
                stats = stats.plus(enqueue(contract, text(item, "clarify_file_id_en"), fallback(text(item, "clarify_file_name_en"), "clarification-request-en-" + reqNo + ".pdf"), HSMT_CLARIFICATION_REQUEST, SOURCE_CLARIFICATION, reqNo, ROLE_REQUEST));
                stats = stats.plus(enqueue(contract, text(item, "clarifyResFileIdEn"), fallback(text(item, "clarifyResFileNameEn"), "clarification-response-en-" + reqNo + ".pdf"), HSMT_CLARIFICATION_RESPONSE, SOURCE_CLARIFICATION, reqNo, ROLE_RESPONSE));
            }
        }
        return stats;
    }

    @Override
    @Transactional
    public DocumentEnqueueStats enqueuePetitionFiles(Contract contract, JsonNode root) {
        DocumentEnqueueStats stats = DocumentEnqueueStats.empty();
        JsonNode versions = root == null ? null : root.get("biduPetitionContractorVersionDTOList");
        if (versions == null || !versions.isArray()) {
            return stats;
        }

        for (JsonNode version : versions) {
            JsonNode items = version.get("biduPetitionContractorDTOList");
            if (items == null || !items.isArray()) {
                continue;
            }
            for (JsonNode item : items) {
                String reqNo = text(item, "reqNo");
                BidPetition petition = upsertPetition(contract, text(version, "notifyVersion"), item);
                upsertPetitionContents(petition, text(item, "content"));
                stats = stats.plus(enqueue(contract, text(item, "reqFileId"), fallback(text(item, "reqFileName"), "petition-request-" + reqNo + ".pdf"), PETITION_REQUEST, SOURCE_PETITION, reqNo, ROLE_REQUEST));
                stats = stats.plus(enqueue(contract, text(item, "resFileId"), fallback(text(item, "resFileName"), "petition-response-" + reqNo + ".pdf"), PETITION_RESPONSE, SOURCE_PETITION, reqNo, ROLE_RESPONSE));
                stats = stats.plus(enqueue(contract, text(item, "cancelFileId"), fallback(text(item, "cancelFileName"), "petition-cancel-" + reqNo + ".pdf"), PETITION_CANCEL, SOURCE_PETITION, reqNo, ROLE_CANCEL));
                stats = stats.plus(enqueuePetitionContentFiles(contract, reqNo, text(item, "content")));
            }
        }
        return stats;
    }

    @Override
    @Transactional
    public DocumentDownloadPendingResult downloadPending(int limit) {
        return download(repository.findByDownloadStatusOrderByCreatedAtAsc(PENDING), null, limit);
    }

    @Override
    @Transactional
    public DocumentDownloadPendingResult downloadPending(Contract contract, int limit) {
        return download(repository.findByContractAndDownloadStatusOrderByCreatedAtAsc(contract, PENDING), null, limit);
    }

    @Override
    @Transactional
    public DocumentDownloadPendingResult downloadPending(Contract contract, String sourcePath, int limit) {
        return download(repository.findByContractAndDownloadStatusOrderByCreatedAtAsc(contract, PENDING), sourcePath, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentSummaryResult summary(Contract contract) {
        List<BiddingDocument> documents = repository.findByContractOrderByCreatedAtAsc(contract);
        int total = documents.size();
        int pending = countByStatus(documents, PENDING);
        int downloading = countByStatus(documents, DOWNLOADING);
        int success = countByStatus(documents, SUCCESS);
        int failed = countByStatus(documents, FAILED);
        double successRate = total == 0 ? 0 : Math.round(success * 10000.0 / total) / 100.0;
        Map<String, Long> byFileType = documents.stream()
                .collect(Collectors.groupingBy(
                        document -> fallback(document.getFileType(), "UNKNOWN"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        List<DocumentFileResult> files = documents.stream()
                .map(document -> new DocumentFileResult(
                        document.getId(),
                        document.getFileExternalId(),
                        document.getFileName(),
                        document.getFileType(),
                        document.getDownloadStatus(),
                        document.getStoragePath(),
                        document.getFileSize(),
                        document.getErrorMessage()
                ))
                .toList();

        return new DocumentSummaryResult(
                contract.getNotifyNo(),
                total,
                pending,
                downloading,
                success,
                failed,
                successRate,
                byFileType,
                files
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClarificationResult> clarifications(Contract contract) {
        return clarificationRepository.findByContractOrderByReqDateDescCreatedAtDesc(contract)
                .stream()
                .map(clarification -> new ClarificationResult(
                        clarification.getReqNo(),
                        clarification.getReqName(),
                        clarification.getReqDate(),
                        clarification.getSignReqDate(),
                        clarification.getSignResDate(),
                        clarificationContentRepository.findByClarificationOrderBySortOrderAsc(clarification)
                                .stream()
                                .map(content -> new ClarificationContentResult(
                                        content.getSubjectCode(),
                                        content.getSubjectName(),
                                        content.getQuestion(),
                                        content.getResponse(),
                                        content.getCatType(),
                                        content.getSortOrder()
                                ))
                                .toList(),
                        repository.findByContractAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                                        contract,
                                        SOURCE_CLARIFICATION,
                                        clarification.getReqNo()
                                )
                                .stream()
                                .map(this::toFileResult)
                                .toList()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetitionResult> petitions(Contract contract) {
        return petitionRepository.findByContractOrderByReqDateDescCreatedAtDesc(contract)
                .stream()
                .map(petition -> new PetitionResult(
                        petition.getReqNo(),
                        petition.getReqName(),
                        petition.getPetitionPeriod(),
                        petition.getReqDate(),
                        petition.getResDate(),
                        petition.getContractorCode(),
                        petition.getContractorName(),
                        petitionContentRepository.findByPetitionOrderBySortOrderAsc(petition)
                                .stream()
                                .map(content -> new PetitionContentResult(
                                        content.getPetitionPeriod(),
                                        content.getReqContent(),
                                        content.getResContent(),
                                        content.getReqDate(),
                                        content.getResDate(),
                                        content.getReplied(),
                                        content.getRestrictCompetition(),
                                        content.getSortOrder()
                                ))
                                .toList(),
                        repository.findByContractAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                                        contract,
                                        SOURCE_PETITION,
                                        petition.getReqNo()
                                )
                                .stream()
                                .map(this::toFileResult)
                                .toList()
                ))
                .toList();
    }

    private DocumentDownloadPendingResult download(List<BiddingDocument> pendingDocuments, String sourcePath, int limit) {
        int total = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;

        List<BiddingDocument> documents = pendingDocuments.stream()
                .limit(Math.max(1, limit))
                .toList();
        if (!documents.isEmpty()) {
            fileService.ensureGatewayReady();
        }

        for (BiddingDocument document : documents) {
            total++;
            if (!hasText(document.getFileExternalId()) || !hasText(document.getFileName())) {
                skipped++;
                document.setDownloadStatus(FAILED);
                document.setErrorMessage("Missing file id or file name");
                repository.save(document);
                continue;
            }

            document.setDownloadStatus(DOWNLOADING);
            document.setErrorMessage(null);
            repository.save(document);

            try {
                FileDownloadResult result = fileService.download(new FileDownloadRequest(
                        document.getFileExternalId(),
                        storageFileName(document),
                        autoDownloadPath(document, sourcePath),
                        relativePath(document.getFileType())
                ));
                document.setStoragePath(result.storagePath());
                document.setFileSize(result.size());
                document.setDownloadedAt(OffsetDateTime.now());
                document.setDownloadStatus(SUCCESS);
                document.setErrorMessage(null);
                success++;
            } catch (Exception ex) {
                document.setDownloadStatus(FAILED);
                document.setErrorMessage(safeMessage(ex));
                log.warn(
                        "Download document failed notifyNo={}, fileId={}, fileName={}, fileType={}, error={}",
                        document.getContract().getNotifyNo(),
                        document.getFileExternalId(),
                        document.getFileName(),
                        document.getFileType(),
                        safeMessage(ex)
                );
                failed++;
            }
            repository.save(document);
        }

        return new DocumentDownloadPendingResult(total, success, failed, skipped);
    }

    private DocumentFileResult toFileResult(BiddingDocument document) {
        return new DocumentFileResult(
                document.getId(),
                document.getFileExternalId(),
                document.getFileName(),
                document.getFileType(),
                document.getDownloadStatus(),
                document.getStoragePath(),
                document.getFileSize(),
                document.getErrorMessage()
        );
    }

    private DocumentEnqueueStats enqueueEvalReportFiles(Contract contract, String evalReportFileInfo) {
        if (!hasText(evalReportFileInfo)) {
            return DocumentEnqueueStats.empty();
        }
        try {
            JsonNode files = objectMapper.readTree(evalReportFileInfo);
            if (!files.isArray()) {
                return DocumentEnqueueStats.empty();
            }
            DocumentEnqueueStats stats = DocumentEnqueueStats.empty();
            for (JsonNode file : files) {
                stats = stats.plus(enqueue(contract, text(file, "fileId"), text(file, "fileName"), E_HSDT_EVAL_REPORT, SOURCE_KQLCNT, null, null));
            }
            return stats;
        } catch (Exception ignored) {
            return DocumentEnqueueStats.empty();
        }
    }

    private DocumentEnqueueStats enqueuePetitionContentFiles(Contract contract, String reqNo, String content) {
        if (!hasText(content)) {
            return DocumentEnqueueStats.empty();
        }
        try {
            JsonNode items = objectMapper.readTree(content);
            if (!items.isArray()) {
                return DocumentEnqueueStats.empty();
            }
            DocumentEnqueueStats stats = DocumentEnqueueStats.empty();
            for (JsonNode item : items) {
                stats = stats.plus(enqueue(contract, text(item, "reqFileId"), fallback(text(item, "reqFileName"), "petition-request-" + reqNo + ".pdf"), PETITION_REQUEST, SOURCE_PETITION, reqNo, ROLE_REQUEST));
                stats = stats.plus(enqueue(contract, text(item, "resFileId"), fallback(text(item, "resFileName"), "petition-response-" + reqNo + ".pdf"), PETITION_RESPONSE, SOURCE_PETITION, reqNo, ROLE_RESPONSE));
            }
            return stats;
        } catch (Exception ignored) {
            return DocumentEnqueueStats.empty();
        }
    }

    private DocumentEnqueueStats enqueue(Contract contract, String fileId, String fileName, String fileType, String sourceType, String sourceRef, String fileRole) {
        if (!hasText(fileId)) {
            return DocumentEnqueueStats.empty();
        }

        BiddingDocument document = repository.findByContractAndFileExternalId(contract, fileId).orElse(null);
        boolean created = document == null;
        if (created) {
            document = new BiddingDocument();
            document.setContract(contract);
            document.setFileExternalId(fileId.trim());
            document.setDownloadStatus(PENDING);
        }

        document.setFileName(fallback(fileName, fileId + ".pdf"));
        document.setFileType(fileType);
        document.setSourceType(sourceType);
        document.setSourceRef(sourceRef);
        document.setFileRole(fileRole);
        if (!SUCCESS.equals(document.getDownloadStatus()) && !DOWNLOADING.equals(document.getDownloadStatus())) {
            document.setDownloadStatus(PENDING);
            document.setErrorMessage(null);
        }
        repository.save(document);
        return new DocumentEnqueueStats(1, created ? 1 : 0, created ? 0 : 1);
    }

    private String autoDownloadPath(BiddingDocument document, String sourcePath) {
        String packageRoot = hasText(sourcePath) ? sourcePath : sourcePath(document.getContract().getNotifyNo());
        return Path.of(packageRoot).resolve(AUTO_DOWNLOAD_FOLDER).toString();
    }

    private String sourcePath(String notifyNo) {
        return syncItemRepository.findByNotifyNo(notifyNo)
                .map(item -> item.getSourcePath())
                .filter(this::hasText)
                .orElseThrow(() -> new IllegalStateException("Missing sourcePath for auto-download folder: " + notifyNo));
    }

    private String relativePath(String fileType) {
        return switch (fileType) {
            case KQLCNT_DECISION -> "kqlcnt";
            case E_HSDT_EVAL_REPORT -> "e-hsdt-eval-report";
            case HSMT_CLARIFICATION_REQUEST, HSMT_CLARIFICATION_RESPONSE -> "clarification";
            case PETITION_REQUEST, PETITION_RESPONSE, PETITION_CANCEL -> "petition";
            default -> "documents";
        };
    }

    private String storageFileName(BiddingDocument document) {
        String fileId = document.getFileExternalId();
        String prefix = fileId.length() <= 8 ? fileId : fileId.substring(0, 8);
        String sourceRef = hasText(document.getSourceRef()) ? document.getSourceRef() + "_" : "";
        String role = hasText(document.getFileRole()) ? document.getFileRole() + "_" : "";
        return sourceRef + role + prefix + "_" + document.getFileName();
    }

    private BidClarification upsertClarification(Contract contract, String notifyVersion, JsonNode item) {
        String reqNo = fallback(text(item, "reqNo"), text(item, "id"));
        BidClarification clarification = clarificationRepository.findByContractAndReqNo(contract, reqNo).orElse(null);
        if (clarification == null) {
            clarification = new BidClarification();
            clarification.setContract(contract);
            clarification.setReqNo(reqNo);
        }
        clarification.setNotifyVersion(notifyVersion);
        clarification.setExternalId(text(item, "id"));
        clarification.setClarifyReqId(text(item, "clarifyReqId"));
        clarification.setReqName(text(item, "reqName"));
        clarification.setReqDate(dateTime(item, "reqDate"));
        clarification.setSignReqDate(dateTime(item, "signReqDate"));
        clarification.setSignResDate(dateTime(item, "signResDate"));
        clarification.setProcuringEntityCode(text(item, "procuringEntityCode"));
        clarification.setProcuringEntityName(text(item, "procuringEntityName"));
        clarification.setPortalCreatedDate(dateTime(item, "createdDate"));
        return clarificationRepository.save(clarification);
    }

    private void upsertClarificationContents(BidClarification clarification, String requestContentJson, String responseContentJson) {
        if (!hasText(requestContentJson) && !hasText(responseContentJson)) {
            return;
        }
        try {
            JsonNode requestContents = parseArray(requestContentJson);
            JsonNode responseContents = parseArray(responseContentJson);
            Map<String, JsonNode> responsesByKey = new LinkedHashMap<>();
            if (responseContents != null) {
                int responseIndex = 0;
                for (JsonNode response : responseContents) {
                    responsesByKey.put(contentKey(response, responseIndex++), response);
                }
            }

            int index = 0;
            if (requestContents != null) {
                for (JsonNode request : requestContents) {
                    String key = contentKey(request, index);
                    JsonNode response = responsesByKey.remove(key);
                    upsertClarificationContent(clarification, key, request, response, index++);
                }
            }

            for (Map.Entry<String, JsonNode> entry : responsesByKey.entrySet()) {
                upsertClarificationContent(clarification, entry.getKey(), null, entry.getValue(), index++);
            }
        } catch (Exception ignored) {
        }
    }

    private JsonNode parseArray(String json) throws Exception {
        if (!hasText(json)) {
            return null;
        }
        JsonNode node = objectMapper.readTree(json);
        return node.isArray() ? node : null;
    }

    private String contentKey(JsonNode content, int index) {
        return fallback(text(content, "id"), String.valueOf(index));
    }

    private void upsertClarificationContent(
            BidClarification clarification,
            String externalId,
            JsonNode request,
            JsonNode response,
            int sortOrder
    ) {
        BidClarificationContent entity = clarificationContentRepository
                .findByClarificationAndExternalId(clarification, externalId)
                .orElse(null);
        if (entity == null) {
            entity = new BidClarificationContent();
            entity.setClarification(clarification);
            entity.setExternalId(externalId);
        }

        JsonNode source = request == null ? response : request;
        if (source == null) {
            return;
        }
        entity.setSubjectCode(text(source, "subjectCode"));
        entity.setSubjectName(text(source, "subjectName"));
        entity.setQuestion(fallback(text(request, "question"), text(response, "question")));
        entity.setResponse(fallback(text(response, "response"), text(request, "response")));
        entity.setCatType(text(source, "catType"));
        entity.setSortOrder(sortOrder);
        clarificationContentRepository.save(entity);
    }

    private void upsertClarificationContents(BidClarification clarification, String contentJson) {
        if (!hasText(contentJson)) {
            return;
        }
        try {
            JsonNode contents = objectMapper.readTree(contentJson);
            if (!contents.isArray()) {
                return;
            }
            int index = 0;
            for (JsonNode content : contents) {
                String externalId = fallback(text(content, "id"), String.valueOf(index));
                BidClarificationContent entity = clarificationContentRepository
                        .findByClarificationAndExternalId(clarification, externalId)
                        .orElse(null);
                if (entity == null) {
                    entity = new BidClarificationContent();
                    entity.setClarification(clarification);
                    entity.setExternalId(externalId);
                }
                entity.setSubjectCode(text(content, "subjectCode"));
                entity.setSubjectName(text(content, "subjectName"));
                entity.setQuestion(text(content, "question"));
                entity.setResponse(text(content, "response"));
                entity.setCatType(text(content, "catType"));
                entity.setSortOrder(index++);
                clarificationContentRepository.save(entity);
            }
        } catch (Exception ignored) {
        }
    }

    private BidPetition upsertPetition(Contract contract, String notifyVersion, JsonNode item) {
        String reqNo = fallback(text(item, "reqNo"), text(item, "id"));
        BidPetition petition = petitionRepository.findByContractAndReqNo(contract, reqNo).orElse(null);
        if (petition == null) {
            petition = new BidPetition();
            petition.setContract(contract);
            petition.setReqNo(reqNo);
        }
        petition.setNotifyVersion(fallback(text(item, "notifyVersion"), notifyVersion));
        petition.setExternalId(text(item, "id"));
        petition.setReqVersion(text(item, "reqVersion"));
        petition.setReqName(text(item, "reqName"));
        petition.setReqDate(dateTime(item, "reqDate"));
        petition.setResDate(dateTime(item, "resDate"));
        petition.setPetitionStatus(text(item, "status"));
        petition.setPetitionPeriod(text(item, "petitionPeriod"));
        petition.setContractorCode(text(item, "contractorCode"));
        petition.setContractorName(text(item, "contractorName"));
        petition.setProcuringEntityCode(text(item, "procuringEntityCode"));
        petition.setProcuringEntityName(text(item, "procuringEntityName"));
        petition.setPortalCreatedDate(dateTime(item, "createdDate"));
        return petitionRepository.save(petition);
    }

    private void upsertPetitionContents(BidPetition petition, String contentJson) {
        if (!hasText(contentJson)) {
            return;
        }
        try {
            JsonNode contents = objectMapper.readTree(contentJson);
            if (!contents.isArray()) {
                return;
            }
            petitionContentRepository.deleteByPetition(petition);
            int index = 0;
            for (JsonNode content : contents) {
                BidPetitionContent entity = new BidPetitionContent();
                entity.setPetition(petition);
                entity.setPetitionPeriod(text(content, "petitionPeriod"));
                entity.setDecision(text(content, "isDecision"));
                entity.setReason(text(content, "reason"));
                entity.setReqContent(text(content, "reqContent"));
                entity.setResContent(text(content, "resContent"));
                entity.setReqDate(dateTime(content, "reqDate"));
                entity.setResDate(dateTime(content, "resDate"));
                entity.setReplied(booleanValue(content, "isReplied"));
                entity.setRestrictCompetition(text(content, "isRestrictCompetition"));
                entity.setSortOrder(index++);
                petitionContentRepository.save(entity);
                if (!hasText(petition.getPetitionPeriod())) {
                    petition.setPetitionPeriod(entity.getPetitionPeriod());
                    petitionRepository.save(petition);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return hasText(text) ? text.trim() : null;
    }

    private String fallback(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private OffsetDateTime dateTime(JsonNode node, String field) {
        String value = text(node, field);
        if (!hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).atZone(ZONE).toOffsetDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText();
        if (!hasText(text)) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private int countByStatus(List<BiddingDocument> documents, String status) {
        return (int) documents.stream()
                .filter(document -> status.equals(document.getDownloadStatus()))
                .count();
    }
}
