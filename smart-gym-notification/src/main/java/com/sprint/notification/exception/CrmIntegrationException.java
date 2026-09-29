package com.sprint.notification.exception;

public class CrmIntegrationException extends RuntimeException {
    public CrmIntegrationException(String message) {
        super(message);
    }

    public CrmIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
