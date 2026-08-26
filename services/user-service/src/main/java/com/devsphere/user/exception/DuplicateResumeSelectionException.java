package com.devsphere.user.exception;

public class DuplicateResumeSelectionException extends RuntimeException {

    private final String code;

    public DuplicateResumeSelectionException(String message) {
        super(message);
        this.code = "DUPLICATE_RESUME_SELECTION";
    }

    public DuplicateResumeSelectionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
