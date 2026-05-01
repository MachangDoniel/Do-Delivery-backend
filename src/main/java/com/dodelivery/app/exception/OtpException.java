package com.dodelivery.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an OTP is invalid, expired, or already used.
 * Results in HTTP 401.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
