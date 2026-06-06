package com.muasamcong.exception;

public class PortalBlockedException extends PortalHttpException {
    public PortalBlockedException(int statusCode, String message) {
        super(statusCode, message);
    }
}
