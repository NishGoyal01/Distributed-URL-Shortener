package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @Operation(summary = "Redirect to the original URL")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
        @ApiResponse(responseCode = "400", description = "Invalid short code"),
        @ApiResponse(responseCode = "404", description = "Short URL not found"),
        @ApiResponse(responseCode = "410", description = "URL expired")
    })
    @GetMapping("/{shortCode:[0-9A-Za-z]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String redirectUrl = urlShortenerService.redirect(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(java.net.URI.create(redirectUrl))
            .build();
    }
}
