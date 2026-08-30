package com.devsphere.user.controller;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.service.PublicResumeService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/resumes")
public class PublicResumeController {

    private static final String CACHE_CONTROL_HEADER = "public, max-age=60, must-revalidate";

    private final PublicResumeService publicResumeService;

    public PublicResumeController(PublicResumeService publicResumeService) {
        this.publicResumeService = publicResumeService;
    }

    @GetMapping("/{publicResumeId}")
    public ResponseEntity<PublicResumeResponse> getPublicResume(
            @PathVariable String publicResumeId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        PublicResumeResponse response = publicResumeService.getPublicResume(publicResumeId);

        String etag = computeEtag(publicResumeId, response);

        if (ifNoneMatch != null && matchesEtag(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER)
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER)
                .body(response);
    }

    private String computeEtag(String publicResumeId, PublicResumeResponse response) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = String.format("%s:%s:%s:%s",
                    publicResumeId,
                    response.getPublishedVersion() != null ? response.getPublishedVersion() : 0,
                    response.getTitle() != null ? response.getTitle() : "",
                    response.getSections() != null ? response.getSections().size() : 0);
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(hash) + "\"";
        } catch (NoSuchAlgorithmException e) {
            return "\"" + publicResumeId + "-v" + (response.getPublishedVersion() != null ? response.getPublishedVersion() : 0) + "\"";
        }
    }

    private boolean matchesEtag(String ifNoneMatch, String etag) {
        String cleanIfNoneMatch = ifNoneMatch.trim();
        if (cleanIfNoneMatch.startsWith("W/")) {
            cleanIfNoneMatch = cleanIfNoneMatch.substring(2).trim();
        }
        return cleanIfNoneMatch.equals(etag) || cleanIfNoneMatch.equals("*");
    }
}
