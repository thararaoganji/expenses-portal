package portal.expenses.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank(message = "Username cannot be blank")
    String username,
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password,
    @NotBlank(message = "Name cannot be blank")
    String name,
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    String email,
    @NotBlank(message = "Role cannot be blank")
    String role) {}