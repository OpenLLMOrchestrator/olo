package com.olo.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check")
public class HealthController {

    @Operation(summary = "Health check")
    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

