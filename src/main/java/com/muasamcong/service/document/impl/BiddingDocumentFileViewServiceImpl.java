package com.muasamcong.service.document.impl;

import com.muasamcong.model.BiddingDocument;
import com.muasamcong.repository.BiddingDocumentRepository;
import com.muasamcong.service.document.BiddingDocumentFileViewService;
import com.muasamcong.service.document.DocumentFileView;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BiddingDocumentFileViewServiceImpl implements BiddingDocumentFileViewService {
    private final BiddingDocumentRepository repository;

    @Override
    public DocumentFileView view(Long id) {
        BiddingDocument document = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!StringUtils.hasText(document.getStoragePath())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document has no downloaded file path");
        }

        Path path = storagePath(document.getStoragePath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
        }
        if (!Files.isReadable(path)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document file is not readable");
        }

        try {
            return new DocumentFileView(
                    new UrlResource(path.toUri()),
                    fileName(document, path),
                    mediaType(path, document.getFileName()),
                    Files.size(path)
            );
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid document file URL", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read document file", ex);
        }
    }

    private Path storagePath(String storagePath) {
        try {
            return Path.of(storagePath.trim()).normalize();
        } catch (InvalidPathException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid document storage path", ex);
        }
    }

    private String fileName(BiddingDocument document, Path path) {
        if (StringUtils.hasText(document.getFileName())) {
            return document.getFileName().trim();
        }
        Path pathFileName = path.getFileName();
        if (pathFileName != null && StringUtils.hasText(pathFileName.toString())) {
            return pathFileName.toString();
        }
        return "document-" + document.getId();
    }

    private MediaType mediaType(Path path, String fileName) {
        String lowerName = StringUtils.hasText(fileName) ? fileName.toLowerCase(Locale.ROOT) : "";
        if (lowerName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lowerName.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        if (lowerName.endsWith(".xls")) {
            return MediaType.parseMediaType("application/vnd.ms-excel");
        }
        if (lowerName.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        if (lowerName.endsWith(".doc")) {
            return MediaType.parseMediaType("application/msword");
        }

        try {
            String probed = Files.probeContentType(path);
            if (StringUtils.hasText(probed)) {
                return MediaType.parseMediaType(probed);
            }
        } catch (Exception ignored) {
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
