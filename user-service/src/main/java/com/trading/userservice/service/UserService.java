package com.trading.userservice.service;

import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.entity.User;
import com.trading.userservice.repository.UserRepository;
import org.apache.kafka.common.errors.DuplicateResourceException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(user ->
                        new UserResponse(user.getId(),
                                user.getEmail(), user.getFirstName(),
                                user.getLastName(), user.getWalletBalance(),
                                user.getStatus(), user.getCreatedAt())).toList();
    }

    public UserResponse registerUser(RegisterRequest payload) {
        User user = userRepository.findByEmail(payload.getEmail());

        if (user != null) {
            throw new DuplicateResourceException("User Already Exists");
        }

        BigDecimal initialBalance = payload.getInitialDeposit() == null
                ? BigDecimal.ZERO
                : payload.getInitialDeposit();

        User savedUser = userRepository.save(new User(
                payload.getEmail(),
                payload.getFirstName(),
                payload.getLastName(),
                payload.getPassword(),
                initialBalance));

        return new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getWalletBalance(),
                savedUser.getStatus(),
                savedUser.getCreatedAt());
    }
}
