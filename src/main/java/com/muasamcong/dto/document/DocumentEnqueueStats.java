package com.muasamcong.dto.document;

public record DocumentEnqueueStats(
        int found,
        int created,
        int existing
) {
    public static DocumentEnqueueStats empty() {
        return new DocumentEnqueueStats(0, 0, 0);
    }

    public DocumentEnqueueStats plus(DocumentEnqueueStats other) {
        return new DocumentEnqueueStats(
                found + other.found(),
                created + other.created(),
                existing + other.existing()
        );
    }
}
