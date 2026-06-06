package com.muasamcong.exception;

public class PortalHttpException extends PortalRequestException {
    private final int statusCode;

    public PortalHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
