package com.muasamcong.service.file.impl;

import com.muasamcong.dto.file.FileDownloadRequest;
import com.muasamcong.dto.file.FileDownloadResult;
import com.muasamcong.service.file.FileService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileServiceImpl implements FileService {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Value("${gateway.url:http://127.0.0.1:18080}")
    private String gatewayUrl;

    @Value("${gateway.key:secret}")
    private String gatewayKey;

    @Value("${storage.root:downloads}")
    private String storageRoot;

    @Override
    public FileDownloadResult download(FileDownloadRequest request) {
        String fileName = cleanFileName(request.fileName());
        String relativePath = cleanPath(request.path());
        Path target = resolve(relativePath).resolve(fileName).normalize();
        ensureInsideRoot(target);

        try {
            Files.createDirectories(target.getParent());
            HttpRequest httpRequest = HttpRequest.newBuilder(downloadUri(request.fileId(), fileName))
                    .timeout(Duration.ofMinutes(3))
                    .header("X-Api-Key", gatewayKey)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gateway HTTP " + response.statusCode());
            }

            try (InputStream inputStream = response.body();
                 OutputStream outputStream = Files.newOutputStream(
                         target,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE
                 )) {
                inputStream.transferTo(outputStream);
            }

            long size = Files.size(target);
            return new FileDownloadResult(
                    "SUCCESS",
                    request.fileId(),
                    fileName,
                    relativePath,
                    target.toAbsolutePath().toString(),
                    size
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot save file: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Download interrupted", ex);
        }
    }

    private Path resolve(String path) {
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path resolved = root.resolve(cleanPath(path)).normalize();
        ensureInsideRoot(resolved);
        return resolved;
    }

    private URI downloadUri(String fileId, String fileName) {
        String url = gatewayUrl.replaceAll("/+$", "")
                + "/download?fileId=" + encode(fileId)
                + "&fileName=" + encode(fileName);
        return URI.create(url);
    }

    private String cleanFileName(String fileName) {
        String cleaned = StringUtils.cleanPath(fileName == null ? "file" : fileName).replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_");
        if (!StringUtils.hasText(cleaned) || cleaned.equals(".") || cleaned.equals("..")) {
            return "file";
        }
        return cleaned;
    }

    private String cleanPath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(path).replace('\\', '/');
        if (cleaned.startsWith("../") || cleaned.equals("..") || cleaned.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path");
        }
        return cleaned;
    }

    private void ensureInsideRoot(Path path) {
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw new IllegalArgumentException("Invalid path");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
