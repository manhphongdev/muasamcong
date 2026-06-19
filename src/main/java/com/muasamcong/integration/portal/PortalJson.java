package com.muasamcong.integration.portal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muasamcong.exception.PortalBlockedException;
import com.muasamcong.exception.PortalHttpException;
import com.muasamcong.exception.PortalRequestException;
import com.muasamcong.exception.PortalTimeoutException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class PortalJson {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode postJson(URI uri, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Origin", "https://muasamcong.mpi.gov.vn")
                    .header("Referer", "https://muasamcong.mpi.gov.vn/")
                    .header("User-Agent", "Mozilla/5.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            int statusCode = response.statusCode();
            if (statusCode == 403 || statusCode == 429) {
                throw new PortalBlockedException(statusCode, "Portal blocked request: HTTP " + statusCode);
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new PortalHttpException(statusCode, "Portal HTTP " + statusCode);
            }

            return objectMapper.readTree(response.body());
        } catch (PortalRequestException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new PortalTimeoutException("Portal request timeout", ex);
        } catch (IOException ex) {
            throw new PortalRequestException("Portal request failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PortalRequestException("Portal request interrupted", ex);
        } catch (Exception ex) {
            throw new PortalRequestException("Cannot parse portal response", ex);
        }
    }

    public String quote(String value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot quote JSON value", ex);
        }
    }
}
