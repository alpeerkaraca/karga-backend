package com.alpeerkaraca.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginRequest {
    @NotBlank(message = "Email can't be empty.")
    @Email(message = "Please provide a valid e-mail adress.")
    private String email;
    @NotBlank(message = "Password can't be empty.")
    private String password;
}
