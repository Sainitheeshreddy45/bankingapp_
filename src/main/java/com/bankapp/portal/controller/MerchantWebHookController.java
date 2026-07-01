package com.bankapp.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/webhooks")
public class MerchantWebHookController {

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MERCHANT_OWNER')")
    public ResponseEntity<?> getWebhookConfiguration() {
        return ResponseEntity.ok(Map.of(
                "webhookUrl", "https://api.merchant.com/v1/callbacks",
                "secretSignKey", "whsec_ABC123XYZ789...",
                "retryPolicy", "EXPONENTIAL_BACKOFF_MAX_5"
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MERCHANT_OWNER')")
    public ResponseEntity<?> saveWebhookConfiguration(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Outbound configuration routes applied to production message loops."
        ));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT_OWNER')")
    public ResponseEntity<?> triggerTestEvent() {
        return ResponseEntity.ok(Map.of(
                "eventId", UUID.randomUUID().toString(),
                "deliveryStatus", "SUCCESS",
                "httpResponseCode", 200,
                "payloadEcho", "{\"event\":\"ping\",\"timestamp\":1781989822}"
        ));
    }
}
