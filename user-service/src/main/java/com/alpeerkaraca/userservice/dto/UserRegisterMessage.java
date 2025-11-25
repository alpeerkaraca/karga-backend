package com.alpeerkaraca.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterMessage {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private double rating;
}
