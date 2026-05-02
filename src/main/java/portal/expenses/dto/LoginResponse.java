package portal.expenses.dto;

import java.util.List;

public record LoginResponse(String token, String username, String name, List<String> roles) {}