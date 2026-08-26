package com.devsphere.user.exception;

public class DuplicateDsaProblemException extends RuntimeException {

    private final String code;

    public DuplicateDsaProblemException(String message) {
        super(message);
        this.code = "DUPLICATE_DSA_PROBLEM";
    }

    public DuplicateDsaProblemException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
