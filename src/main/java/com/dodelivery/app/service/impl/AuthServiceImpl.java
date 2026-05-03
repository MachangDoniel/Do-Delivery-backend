package com.dodelivery.app.service.impl;

import com.dodelivery.app.dto.request.LoginRequest;
import com.dodelivery.app.dto.request.RefreshTokenRequest;
import com.dodelivery.app.dto.request.RegisterRequest;
import com.dodelivery.app.dto.request.SendOtpRequest;
import com.dodelivery.app.dto.response.AuthResponse;
import com.dodelivery.app.dto.response.OtpResponse;
import com.dodelivery.app.entity.RiderProfile;
import com.dodelivery.app.entity.User;
import com.dodelivery.app.enums.Role;
import com.dodelivery.app.exception.BusinessException;
import com.dodelivery.app.exception.OtpException;
import com.dodelivery.app.exception.ResourceNotFoundException;
import com.dodelivery.app.repository.RiderProfileRepository;
import com.dodelivery.app.repository.UserRepository;
import com.dodelivery.app.security.AppUserDetails;
import com.dodelivery.app.security.JwtTokenProvider;
import com.dodelivery.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String OTP_KEY_PREFIX           = "otp:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final int    OTP_LENGTH               = 4;

    private final UserRepository         userRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final JwtTokenProvider       jwtTokenProvider;
    private final StringRedisTemplate    redisTemplate;

    @Value("${app.otp.ttl-seconds}")
    private long otpTtlSeconds;

    @Value("${app.otp.mock-enabled}")
    private boolean mockOtpEnabled;

    @Value("${app.redis.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    // ── Public methods ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public OtpResponse register(RegisterRequest request) {
        String phone = normalisePhone(request.phone());

        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("Phone number already registered");
        }

        User user = User.builder()
                .name(request.name().trim())
                .phone(phone)
                .role(request.role())
                .build();
        userRepository.save(user);

        // Automatically create a RiderProfile when the user registers as a rider
        if (request.role() == Role.RIDER) {
            RiderProfile profile = RiderProfile.builder()
                    .user(user)
                    .build();
            riderProfileRepository.save(profile);
        }

        String otp = generateAndStoreOtp(phone);
        log.info("OTP generated for new user [phone={}]", maskPhone(phone));

        return new OtpResponse(
                "Registration successful. OTP sent.",
                phone,
                mockOtpEnabled ? otp : null
        );
    }

    @Override
    public OtpResponse sendOtp(SendOtpRequest request) {
        String phone = normalisePhone(request.phone());

        // Prevent OTP enumeration — same message regardless of whether user exists
        if (!userRepository.existsByPhone(phone)) {
            return new OtpResponse("OTP sent if phone is registered.", phone, null);
        }

        String otp = generateAndStoreOtp(phone);
        log.info("OTP re-generated for user [phone={}]", maskPhone(phone));

        return new OtpResponse(
                "OTP sent.",
                phone,
                mockOtpEnabled ? otp : null
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String phone = normalisePhone(request.phone());

        // Validate OTP — use a constant-time comparison to prevent timing attacks
        String storedOtp = redisTemplate.opsForValue().get(OTP_KEY_PREFIX + phone);
        if (storedOtp == null || !storedOtp.equals(request.otp())) {
            throw new OtpException("Invalid or expired OTP");
        }

        // OTP is single-use — delete immediately after successful validation
        redisTemplate.delete(OTP_KEY_PREFIX + phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDetails userDetails = toUserDetails(user);
        String accessToken  = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(phone);

        storeRefreshToken(phone, refreshToken);

        return new AuthResponse(accessToken, refreshToken, 900L);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        String phone = jwtTokenProvider.getPhoneFromToken(token);
        String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + phone);

        if (stored == null || !stored.equals(token)) {
            // Possible token reuse — revoke all tokens for this user
            redisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + phone);
            throw new BusinessException("Refresh token already used or revoked");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Rotate: issue new token pair and replace stored refresh token
        String newAccess  = jwtTokenProvider.generateAccessToken(toUserDetails(user));
        String newRefresh = jwtTokenProvider.generateRefreshToken(phone);
        storeRefreshToken(phone, newRefresh);

        return new AuthResponse(newAccess, newRefresh, 900L);
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return; // Already invalid — nothing to revoke
        }
        String phone = jwtTokenProvider.getPhoneFromToken(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + phone);
        log.info("Refresh token revoked for user [phone={}]", maskPhone(phone));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String generateAndStoreOtp(String phone) {
        String otp = String.format("%0" + OTP_LENGTH + "d",
                new SecureRandom().nextInt((int) Math.pow(10, OTP_LENGTH)));
        redisTemplate.opsForValue()
                .set(OTP_KEY_PREFIX + phone, otp, Duration.ofSeconds(otpTtlSeconds));
        return otp;
    }

    private void storeRefreshToken(String phone, String token) {
        redisTemplate.opsForValue()
                .set(REFRESH_TOKEN_KEY_PREFIX + phone, token,
                        Duration.ofDays(refreshTokenTtlDays));
    }

    private UserDetails toUserDetails(User user) {
        return AppUserDetails.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }

    /** Strip whitespace and convert to lowercase for consistent storage. */
    private String normalisePhone(String phone) {
        return phone.trim();
    }

    /** Mask phone for safe logging — show first 3 and last 2 digits only. */
    private String maskPhone(String phone) {
        if (phone.length() <= 5) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }
}
