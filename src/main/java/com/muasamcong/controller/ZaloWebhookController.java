package com.muasamcong.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class ZaloWebhookController {

    private static final String WEBHOOK_SECRET_TOKEN = "mykey-abcyxz";

    @PostMapping("/webhooks")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "X-Bot-Api-Secret-Token", required = false) String secretToken,
            @RequestBody Map<String, Object> body
    ) {
        if (!"mykey-abcyxz".equals(secretToken)) {
            return ResponseEntity.status(403).body(Map.of("message", "Unauthorized"));
        }

        Map<String, Object> result = (Map<String, Object>) body.get("result");
        Map<String, Object> message = (Map<String, Object>) result.get("message");
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");

        String chatId = (String) chat.get("id");

        log.info("CHAT_ID = " + chatId);

        return ResponseEntity.ok(Map.of(
                "message", "Success",
                "chat_id", chatId
        ));
    }
}