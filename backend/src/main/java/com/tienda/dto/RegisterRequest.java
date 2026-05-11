package com.tienda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    @NotBlank
    private String fullName;

    @NotBlank
    private String role;

    private String storeName;
    private String storeAddress;

    private String companyName;
    private String contactPhone;
    private String emergencyEmail;
    private String address;
}
