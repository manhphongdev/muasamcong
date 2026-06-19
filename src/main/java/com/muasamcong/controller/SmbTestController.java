package com.muasamcong.controller;

import com.muasamcong.service.storage.StoredFile;
import com.muasamcong.service.storage.SyncStorageService;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/smb")
public class SmbTestController {
    private final SmbSessionFactory smbSessionFactory;
    private final SyncStorageService syncStorageService;

    @GetMapping("/test")
    public String testConnection(@RequestParam(defaultValue = "") String path) throws IOException {
        try (var session = smbSessionFactory.getSession()) {
            String[] files = session.listNames(path.trim());
            return Arrays.toString(files);
        }
    }

    @PostMapping("/write-test")
    public StoredFile writeTest(
            @RequestParam String path,
            @RequestParam(defaultValue = "smb-write-test.txt") String fileName
    ) {
        String content = "SMB write test at " + OffsetDateTime.now() + System.lineSeparator();
        return syncStorageService.write(
                path.trim(),
                "",
                fileName.trim(),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
    }
}
