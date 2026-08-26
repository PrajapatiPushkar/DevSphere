package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateDsaProblemRequest;
import com.devsphere.user.dto.DailyDsaProgressResponse;
import com.devsphere.user.dto.DsaProblemResponse;
import com.devsphere.user.dto.DsaStatisticsResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.UpdateDsaProblemRequest;
import com.devsphere.user.entity.DsaDifficulty;
import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaProblemStatus;
import com.devsphere.user.entity.DsaTopic;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.DsaProblemService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dsa")
public class DsaProblemController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final DsaProblemService dsaProblemService;

    public DsaProblemController(DsaProblemService dsaProblemService) {
        this.dsaProblemService = dsaProblemService;
    }

    @PostMapping("/problems")
    public ResponseEntity<DsaProblemResponse> createProblem(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @Valid @RequestBody CreateDsaProblemRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.createProblem(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/dsa/problems/" + response.getId())).body(response);
    }

    @GetMapping("/problems")
    public ResponseEntity<PageResponse<DsaProblemResponse>> listProblems(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @RequestParam(value = "difficulty", required = false) DsaDifficulty difficulty,
            @RequestParam(value = "topic", required = false) DsaTopic topic,
            @RequestParam(value = "platform", required = false) DsaPlatform platform,
            @RequestParam(value = "status", required = false) DsaProblemStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PageResponse<DsaProblemResponse> response = dsaProblemService.listProblems(userId, difficulty, topic, platform, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<DsaProblemResponse> getProblemById(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.getProblem(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/problems/{id}")
    public ResponseEntity<DsaProblemResponse> updateProblem(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId,
            @Valid @RequestBody UpdateDsaProblemRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.updateProblem(userId, problemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/problems/{id}")
    public ResponseEntity<Void> archiveProblem(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        dsaProblemService.archiveProblem(userId, problemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/problems/{id}/attempt")
    public ResponseEntity<DsaProblemResponse> incrementAttempt(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.incrementAttempt(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/problems/{id}/start")
    public ResponseEntity<DsaProblemResponse> startProblem(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.startProblem(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/problems/{id}/solve")
    public ResponseEntity<DsaProblemResponse> solveProblem(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.solveProblem(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/problems/{id}/revisit")
    public ResponseEntity<DsaProblemResponse> revisitProblem(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long problemId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaProblemResponse response = dsaProblemService.revisitProblem(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/progress/daily")
    public ResponseEntity<DailyDsaProgressResponse> getDailyProgress(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DailyDsaProgressResponse response = dsaProblemService.getDailyProgress(userId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<DsaStatisticsResponse> getStatistics(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DsaStatisticsResponse response = dsaProblemService.getStatistics(userId);
        return ResponseEntity.ok(response);
    }

    private Long extractAndValidateUserId(String authUserIdHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        if (authUserIdHeader != null && !authUserIdHeader.isBlank()) {
            try {
                return Long.parseLong(authUserIdHeader.trim());
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("Invalid authenticated user identity format");
            }
        }

        throw new UnauthorizedException("Authenticated user identity is required");
    }
}
