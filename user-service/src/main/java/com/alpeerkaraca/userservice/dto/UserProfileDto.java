package com.alpeerkaraca.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    @Size(min = 1, max = 255)
    @NotBlank(message = "Name can't be empty.")
    private String firstName;
    @Size(min = 1, max = 255)
    @NotBlank(message = "Name can't be empty.")
    private String lastName;
    @NotBlank(message = "Email can't be empty.")
    @Email(message = "Please provide a valid e-mail adress.")
    private String email;
    @NotEmpty(message = "Phone number can't be empty.")
    @Size(min = 12, max = 13, message = "Phone number should include country code.")
    private String phoneNumber;
}
