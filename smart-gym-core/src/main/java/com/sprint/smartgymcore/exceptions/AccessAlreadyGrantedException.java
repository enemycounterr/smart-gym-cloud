package com.sprint.smartgymcore.exceptions;

public class AccessAlreadyGrantedException extends RuntimeException {
    public AccessAlreadyGrantedException(String message) {
        super(message);
    }
}
