package com.muasamcong.service.file.impl;

import com.muasamcong.dto.file.FileDownloadRequest;
import com.muasamcong.dto.file.FileDownloadResult;
import com.muasamcong.service.file.FileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final int MAX_ERROR_BODY_LENGTH = 1000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gateway.url:http://127.0.0.1:18080}")
    private String gatewayUrl;

    @Value("${gateway.key:secret}")
    private String gatewayKey;

    @Override
    public void ensureGatewayReady() {
        URI healthUri = URI.create(gatewayUrl.replaceAll("/+$", "") + "/health");
        HttpRequest httpRequest = HttpRequest.newBuilder(healthUri)
                .timeout(Duration.ofSeconds(5))
                .header("X-Api-Key", gatewayKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gateway chua san sang: " + healthUri + " tra ve HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String status = text(root, "status");
            if (!"OK".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Gateway chua san sang: health status=" + fallback(status, "UNKNOWN"));
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gateway health check bi gian doan", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Gateway chua san sang tai " + healthUri + ": " + safeMessage(ex), ex);
        }
    }

    @Override
    public FileDownloadResult download(FileDownloadRequest request) {
        String fileName = cleanFileName(request.fileName());
        String relativePath = cleanPath(request.relativePath());
        Path basePath = basePath(request.basePath());
        Path target = basePath.resolve(relativePath).resolve(fileName).normalize();
        ensureInsideBasePath(basePath, target);

        try {
            Files.createDirectories(target.getParent());
            HttpRequest httpRequest = HttpRequest.newBuilder(downloadUri(request.fileId(), fileName))
                    .timeout(Duration.ofMinutes(3))
                    .header("X-Api-Key", gatewayKey)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(gatewayErrorMessage(response));
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
            throw new IllegalStateException("Cannot save file: " + safeMessage(ex), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Download interrupted", ex);
        }
    }

    private Path basePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("basePath is required");
        }
        return Path.of(path.trim()).toAbsolutePath().normalize();
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

    private void ensureInsideBasePath(Path basePath, Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid path");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String gatewayErrorMessage(HttpResponse<InputStream> response) {
        String body = readErrorBody(response.body());
        if (!StringUtils.hasText(body)) {
            return "Gateway HTTP " + response.statusCode();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            String error = text(root, "error");
            String message = text(root, "message");
            String agentStatus = text(root, "agentStatus");
            String agentBody = text(root, "agentBody");
            StringBuilder detail = new StringBuilder("Gateway HTTP ").append(response.statusCode());
            if (StringUtils.hasText(error)) {
                detail.append(" [").append(error).append("]");
            }
            if (StringUtils.hasText(message)) {
                detail.append(": ").append(message);
            }
            if (StringUtils.hasText(agentStatus)) {
                detail.append("; agentStatus=").append(agentStatus);
            }
            if (StringUtils.hasText(agentBody)) {
                detail.append("; agentBody=").append(truncate(agentBody));
            }
            return detail.toString();
        } catch (Exception ignored) {
            return "Gateway HTTP " + response.statusCode() + ": " + truncate(body);
        }
    }

    private String readErrorBody(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try (inputStream) {
            return truncate(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return "Cannot read gateway error body: " + ex.getMessage();
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isTextual() ? value.asText() : value.toString();
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
    }
}
