package portal.expenses.dto;

public record RegisterRequest(String username, String password, String name, String email, String role) {
}
