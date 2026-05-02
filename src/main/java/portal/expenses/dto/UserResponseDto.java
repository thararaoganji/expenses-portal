package portal.expenses.dto;

import java.util.Set;

public record UserResponseDto(
    Long id,
    String username,
    String name,
    String email,
    Set<String> roles
) {
}