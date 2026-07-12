package com.seatsure.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "First Name is required ")
    @Size(max = 30, message = "First Name cannot exceed 30 characters ")
    private String firstName;

    @Size(max = 30, message = "Last name cannot exceeds 30 characters")
    private String lastName;

    @Email(message = "Please Enter a vaild Email address")
    @NotBlank(message = "Email is required")
    @Size(max = 255)
    private String email;

    @NotBlank(message = " Password is Required")
    @Size(min = 8, max = 35, message = "Password must be between 8 to 100 characters")
    private String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone Number mut be exactly 10 digits")
    private String phone;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
