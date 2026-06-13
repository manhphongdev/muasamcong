package com.muasamcong.service.file;

import com.muasamcong.dto.file.FileDownloadRequest;
import com.muasamcong.dto.file.FileDownloadResult;

public interface FileService {
    FileDownloadResult download(FileDownloadRequest request);
}
