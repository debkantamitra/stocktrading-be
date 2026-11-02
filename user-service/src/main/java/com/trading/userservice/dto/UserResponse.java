package com.trading.userservice.dto;

import com.trading.userservice.entity.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;

    private String email;

    private String firstName;

    private String lastName;

    private BigDecimal walletBalance;

    private UserStatus status;

    private LocalDateTime createdAt;
}
