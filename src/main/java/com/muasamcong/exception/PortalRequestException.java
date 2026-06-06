package com.muasamcong.exception;

public class PortalRequestException extends RuntimeException {
    public PortalRequestException(String message) {
        super(message);
    }

    public PortalRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
