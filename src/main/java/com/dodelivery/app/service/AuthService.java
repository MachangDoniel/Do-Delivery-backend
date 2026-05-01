package com.dodelivery.app.service;

import com.dodelivery.app.dto.request.LoginRequest;
import com.dodelivery.app.dto.request.RefreshTokenRequest;
import com.dodelivery.app.dto.request.RegisterRequest;
import com.dodelivery.app.dto.request.SendOtpRequest;
import com.dodelivery.app.dto.response.AuthResponse;
import com.dodelivery.app.dto.response.OtpResponse;

public interface AuthService {

    /** Register a new user and issue a mock OTP. */
    OtpResponse register(RegisterRequest request);

    /** Request a fresh OTP for an existing user (re-login). */
    OtpResponse sendOtp(SendOtpRequest request);

    /** Verify OTP and exchange it for JWT access + refresh tokens. */
    AuthResponse login(LoginRequest request);

    /** Rotate a refresh token and return a new token pair. */
    AuthResponse refresh(RefreshTokenRequest request);
}
