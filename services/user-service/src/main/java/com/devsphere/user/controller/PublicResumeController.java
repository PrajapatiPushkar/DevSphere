package com.devsphere.user.controller;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.service.PublicResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/resumes")
public class PublicResumeController {

    private final PublicResumeService publicResumeService;

    public PublicResumeController(PublicResumeService publicResumeService) {
        this.publicResumeService = publicResumeService;
    }

    @GetMapping("/{publicResumeId}")
    public ResponseEntity<PublicResumeResponse> getPublicResume(@PathVariable String publicResumeId) {
        PublicResumeResponse response = publicResumeService.getPublicResume(publicResumeId);
        return ResponseEntity.ok(response);
    }
}
