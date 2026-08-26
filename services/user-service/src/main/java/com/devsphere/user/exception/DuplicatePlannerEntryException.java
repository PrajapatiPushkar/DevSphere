package com.devsphere.user.exception;

public class DuplicatePlannerEntryException extends RuntimeException {

    private final String code;

    public DuplicatePlannerEntryException(String message) {
        super(message);
        this.code = "DUPLICATE_PLANNER_ENTRY";
    }

    public DuplicatePlannerEntryException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
