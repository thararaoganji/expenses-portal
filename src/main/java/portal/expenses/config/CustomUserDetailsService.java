package portal.expenses.config;

import org.springframework.stereotype.Component;

@Component("customUserDetailsServiceConfig")
public class CustomUserDetailsService {

    public String getInfo() {
        return "CustomUserDetailsService";
    }
}
