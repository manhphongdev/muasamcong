package com.muasamcong.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/smb")
public class SmbTestController {
    private final SmbSessionFactory smbSessionFactory;

    @GetMapping("/test")
    public String testConnection() throws IOException {
        try (var session = smbSessionFactory.getSession()) {
            String[] files = session.listNames("07. TAI LIEU CA NHAN");
            return Arrays.toString(files);
        }
    }
}
