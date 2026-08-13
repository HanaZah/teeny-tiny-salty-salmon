package com.finadvise.crm.common;

public class SystemIntegrityException extends RuntimeException {
    public SystemIntegrityException(String message) {
        super(message);
    }

    public SystemIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
