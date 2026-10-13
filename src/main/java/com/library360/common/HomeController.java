package com.library360.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<?> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/swagger-ui/index.html"))
            .body(Map.of(
                "status", "UP",
                "message", "Library360 API is active and running",
                "docs", "/swagger-ui/index.html",
                "health", "/actuator/health"
            ));
    }
}
