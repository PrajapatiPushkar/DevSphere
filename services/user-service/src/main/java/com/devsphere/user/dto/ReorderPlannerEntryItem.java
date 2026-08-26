package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReorderPlannerEntryItem {

    @NotNull(message = "entryId is required")
    private Long entryId;

    @NotNull(message = "sortOrder is required")
    @Min(value = 0, message = "sortOrder must be zero or positive")
    private Integer sortOrder;

    public ReorderPlannerEntryItem() {
    }

    public ReorderPlannerEntryItem(Long entryId, Integer sortOrder) {
        this.entryId = entryId;
        this.sortOrder = sortOrder;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
