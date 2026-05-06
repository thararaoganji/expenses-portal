package portal.expenses.config;

import portal.expenses.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger jwtAuthFilterLogger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        String requestPath = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // If there's no Bearer token in the header, pass the request to the next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            jwtAuthFilterLogger.debug("No JWT token found in request to: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the token and username.
        jwt = authHeader.substring(7);
        try {
            username = jwtUtil.extractUsername(jwt);
            jwtAuthFilterLogger.debug("JWT token found for user: {} accessing: {}", username, requestPath);
        } catch (JwtException e) {
            // If the token is invalid (e.g., expired, malformed), send a 403 response.
            jwtAuthFilterLogger.warn("Invalid or expired JWT token for request to: {} - Error: {}", requestPath, e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired JWT token\", \"message\": \"" + e.getMessage() + "\"}");
            return;
        }

        // If we have a username but the user is not yet authenticated,
        // validate the token and set the authentication in the security context.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) { //NOSONAR
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    jwtAuthFilterLogger.debug("Authentication successful for user: {} with roles: {}", username, userDetails.getAuthorities());
                } else {
                    jwtAuthFilterLogger.warn("JWT token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                jwtAuthFilterLogger.error("Error during authentication for user: {} - Error: {}", username, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
