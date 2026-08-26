package com.devsphere.user.exception;

public class DuplicateSkillException extends RuntimeException {

    private final String code;

    public DuplicateSkillException(String message) {
        super(message);
        this.code = "DUPLICATE_SKILL";
    }

    public DuplicateSkillException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
