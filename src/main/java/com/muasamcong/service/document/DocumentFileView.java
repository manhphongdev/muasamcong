package com.muasamcong.service.document;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record DocumentFileView(
        Resource resource,
        String fileName,
        MediaType mediaType,
        long contentLength
) {
}
