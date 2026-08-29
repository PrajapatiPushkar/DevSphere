package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.UpdateGoalRequest;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.GoalService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @Valid @RequestBody CreateGoalRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        GoalResponse response = goalService.createGoal(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/goals/" + response.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<GoalResponse>> getGoals(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @RequestParam(value = "status", required = false) GoalStatus status,
            @RequestParam(value = "goalType", required = false) GoalType goalType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PageResponse<GoalResponse> response = goalService.getGoals(userId, status, goalType, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long goalId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        GoalResponse response = goalService.getGoalById(userId, goalId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        GoalResponse response = goalService.updateGoal(userId, goalId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long goalId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        goalService.archiveGoal(userId, goalId);
        return ResponseEntity.noContent().build();
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
