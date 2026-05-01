package com.dodelivery.app.dto.response;

/**
 * Returned after initiating OTP registration or requesting a new OTP.
 * In production, 'otp' is null (SMS is sent instead).
 * In mock/dev mode the OTP is included so the client can proceed without a real SMS gateway.
 */
public record OtpResponse(
        String message,
        String phone,
        String otp          // null in production; populated only when app.otp.mock-enabled=true
) {}
