package com.muasamcong.service.document;

import com.muasamcong.dto.document.DocumentDownloadPendingResult;

public interface DocumentDownloadWorkerService {
    DocumentDownloadPendingResult downloadPending(int limit);
}
