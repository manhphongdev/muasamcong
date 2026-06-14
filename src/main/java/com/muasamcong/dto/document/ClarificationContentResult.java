package com.muasamcong.dto.document;

public record ClarificationContentResult(
        String subjectCode,
        String subjectName,
        String question,
        String response,
        String catType,
        Integer sortOrder
) {
}
