package com.trading.userservice.service;

import com.trading.userservice.dto.AuthResponse;
import com.trading.userservice.dto.LoginRequest;
import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.entity.User;
import com.trading.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    /**
     * Register a new trader
     * @param request
     * @return AuthResponse
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("registering user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email Already Exists: " + request.getEmail());
        }

        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .walletBalance(request.getInitialDeposit() != null ?
                        request.getInitialDeposit() :
                        BigDecimal.valueOf(10000))
                .build();

        User savedUser = userRepository.save(newUser);

        log.info("User registered: {}", savedUser.getId());

        // publish user.registered event
        Map<String, Object> userRegisteredEvent = new HashMap<>();

        userRegisteredEvent.put("userId", savedUser.getId());
        userRegisteredEvent.put("email", savedUser.getEmail());
        userRegisteredEvent.put("firstName", savedUser.getFirstName());
        userRegisteredEvent.put("lastName", savedUser.getLastName());
        userRegisteredEvent.put("walletBalance", savedUser.getWalletBalance());

        kafkaTemplate.send(USER_REGISTERED_TOPIC, savedUser.getId(), userRegisteredEvent);

        // token generation and auth response
        String token = generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = generateRefreshToken(savedUser.getId());

        return buildAuthResponse(savedUser, token, refreshToken);
    }

    /**
     * Log in
     * @param request
     * @return AuthResponse
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        log.info("Login successful: {}", user.getId());

        // token generation and auth response
        String token = generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = generateRefreshToken(user.getId());

        return buildAuthResponse(user, token, refreshToken);
    }

    /**
     * Get User By Id
     * Called by other services via Feign (internal)
     * @param userId
     * @return
     */
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return mapUserToUserResponse(user);
    }

    /**
     * Adds funds to wallet
     * @param userId
     * @param amount
     * @return
     */
    public UserResponse addFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser = userRepository.save(user);

        log.info("Funds added: {} to user: {}", amount, user.getId());

        return mapUserToUserResponse(savedUser);
    }

    /**
     * Deducts funds from wallet - called on BUY order
     * @param userId
     * @param amount
     * @return
     */
    public UserResponse deductFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        User savedUser = userRepository.save(user);

        log.info("Funds deducted: {} to user: {}", amount, user.getId());

        return mapUserToUserResponse(savedUser);
    }

    /**
     * Credits funds to wallet - called on SELL order
     * @param userId
     * @param amount
     * @return
     */
    public UserResponse creditFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser = userRepository.save(user);

        log.info("Funds credited: {} to user: {}", amount, user.getId());

        return mapUserToUserResponse(savedUser);
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .walletBalance(user.getWalletBalance())
                .accessToken(token)
                .refreshToken(refreshToken)
                .build();
    }

    private UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .walletBalance(user.getWalletBalance())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
