package com.alpeerkaraca.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegisterRequest {
    @NotBlank(message = "Name can't be empty.")
    @Size(min = 2, max = 64)
    String firstName;

    @NotBlank(message = "Lastname can't be empty.")
    @Size(min = 2, max = 64)
    String lastName;

    @NotBlank(message = "E-Mail can't be empty.")
    @Size(min = 2, max = 256)
    @Email(message = "Please provide a valid e-maill adress.")
    String email;

    @NotBlank(message = "Password can't be empty.")
    @Size(min = 8, max = 64, message = "Password must be between 8 - 64 characters.")
    String password;

    @NotBlank
    @Size(min = 13, max = 15, message = "Phone number should include country code.")
    String phoneNumber;

}
