package portal.expenses.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // This is invoked when an unauthenticated user tries to access a protected resource.
        String requestUri = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        logger.warn("Unauthorized access attempt to: {} - Auth header present: {} - Exception: {}",
                   requestUri, (authHeader != null), authException.getMessage());

        // We send a 401 Unauthorized response.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Access is denied due to invalid credentials\", \"path\": \"" + requestUri + "\"}");
    }
}
