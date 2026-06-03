package com.example.job_search.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDto {

    @NotBlank(message = "Ім'я не може бути порожнім")
    @Size(min = 2, max = 50, message = "Ім'я має містити від 2 до 50 символів")
    private String name;

    @NotBlank(message = "Email обов'язковий")
    @Email(message = "Некоректний формат електронної пошти")
    private String email;

    @NotBlank(message = "Пароль обов'язковий")
    @Size(min = 6, message = "Пароль має містити мінімум 6 символів")
    private String password;

    @NotBlank(message = "Підтвердження пароля обов'язкове")
    private String passwordConfirm;
}