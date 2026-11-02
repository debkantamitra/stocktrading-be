package com.trading.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "First Name is a required field")
    private String firstName;

    @NotBlank(message = "Last Name is a required field")
    private String lastName;

    @NotBlank(message = "Email is a required field")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is a required field")
    @Size(min = 6, message = "Password should be of at least length 6")
    private String password;

    private BigDecimal initialDeposit;
}
