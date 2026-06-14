package com.muasamcong.dto.document;

public record DocumentEnqueueResult(
        String notifyNo,
        int enqueued,
        int skipped
) {
}
