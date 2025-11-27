package com.alpeerkaraca.userservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserProfileDto {

    @Size(min = 1, max = 255)
    private String firstName;

    @Size(min = 1, max = 255)
    private String lastName;

    @Email(message = "Please provide a valid e-mail address.")
    private String email;

    @Size(min = 12, max = 13, message = "Phone number should include country code.")
    private String phoneNumber;

    private Double rating;

    @JsonIgnore
    public boolean isAllFieldsNull() {
        return firstName == null
                && lastName == null
                && email == null
                && phoneNumber == null
                && rating == null;
    }
}