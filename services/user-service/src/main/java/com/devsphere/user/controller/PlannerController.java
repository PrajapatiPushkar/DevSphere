package com.devsphere.user.controller;

import com.devsphere.user.dto.CreatePlannerEntryRequest;
import com.devsphere.user.dto.DailyPlannerResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.PlannerEntryResponse;
import com.devsphere.user.dto.ReorderPlannerEntryItem;
import com.devsphere.user.dto.ReschedulePlannerEntryRequest;
import com.devsphere.user.dto.UpdatePlannerEntryRequest;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.PlannerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/v1/planner")
public class PlannerController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final PlannerService plannerService;

    public PlannerController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping("/entries")
    public ResponseEntity<PlannerEntryResponse> createPlannerEntry(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @Valid @RequestBody CreatePlannerEntryRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PlannerEntryResponse response = plannerService.createPlannerEntry(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/planner/entries/" + response.getId())).body(response);
    }

    @GetMapping("/entries")
    public ResponseEntity<PageResponse<PlannerEntryResponse>> listPlannerEntries(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "plannedDate,desc") String sort) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PageResponse<PlannerEntryResponse> response = plannerService.listPlannerEntries(userId, date, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<PlannerEntryResponse> getPlannerEntryById(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long entryId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PlannerEntryResponse response = plannerService.getPlannerEntry(userId, entryId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<PlannerEntryResponse> updatePlannerEntry(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long entryId,
            @Valid @RequestBody UpdatePlannerEntryRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PlannerEntryResponse response = plannerService.updatePlannerEntry(userId, entryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> unschedulePlannerEntry(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long entryId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        plannerService.unschedulePlannerEntry(userId, entryId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/entries/{id}/reschedule")
    public ResponseEntity<PlannerEntryResponse> reschedulePlannerEntry(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long entryId,
            @Valid @RequestBody ReschedulePlannerEntryRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PlannerEntryResponse response = plannerService.reschedulePlannerEntry(userId, entryId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/days/{date}")
    public ResponseEntity<DailyPlannerResponse> getDailyPlanner(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DailyPlannerResponse response = plannerService.getDailyPlanner(userId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<DailyPlannerResponse> getTodayPlanner(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DailyPlannerResponse response = plannerService.getTodayPlanner(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/days/{date}/reorder")
    public ResponseEntity<DailyPlannerResponse> reorderDay(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody List<ReorderPlannerEntryItem> reorderItems) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        DailyPlannerResponse response = plannerService.reorderDay(userId, date, reorderItems);
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
