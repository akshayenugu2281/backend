package com.library360.common;

import lombok.Getter;

/**
 * Business exception carrying a typed error code for consistent error handling.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public BusinessException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }
}
