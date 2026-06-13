package com.muasamcong.controller;

import com.muasamcong.dto.ApiResponse;
import com.muasamcong.dto.file.FileDownloadRequest;
import com.muasamcong.dto.file.FileDownloadResult;
import com.muasamcong.service.file.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/download")
    public ApiResponse<FileDownloadResult> download(@Valid @RequestBody FileDownloadRequest request) {
        return ApiResponse.success(fileService.download(request));
    }
}
