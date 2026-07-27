package portal.expenses.controller;

import portal.expenses.dto.LoginRequest;
import portal.expenses.dto.LoginResponse;
import portal.expenses.entity.AppUser;
import portal.expenses.repository.UserRepository;
import portal.expenses.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String VALID_KEY = "valid";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UserDetailsService userDetailsService, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.username());

        // This will trigger the DaoAuthenticationProvider to validate the credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        // If authentication is successful, generate a JWT
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        // Extract roles from authorities
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        logger.info("User logged in successfully: {} with roles: {}", userDetails.getUsername(), roles);
        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
        return new LoginResponse(token, userDetails.getUsername(),user.getName(), roles);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put(VALID_KEY, false);
                response.put("error", "Missing or invalid Authorization header");
                logger.warn("Token validation failed: Missing or invalid header");
                return ResponseEntity.badRequest().body(response);
            }

            String token = authHeader.substring(7);
            if (logger.isInfoEnabled()) {
                String truncatedToken = token.substring(0, Math.min(token.length(), 30)) + "...";
                logger.info("Validating token: {}", truncatedToken);
            }

            String username = jwtUtil.extractUsername(token);
            response.put("username", username);
            logger.info("Extracted username from token: {}", username);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            boolean isValid = jwtUtil.validateToken(token, userDetails);

            response.put(VALID_KEY, isValid);
            response.put("roles", userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());

            if (isValid) {
                logger.info("Token is VALID for user: {} with roles: {}", username, userDetails.getAuthorities());
            } else {
                logger.warn("Token is INVALID for user: {}", username);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put(VALID_KEY, false);
            response.put("error", e.getMessage());
            logger.error("Token validation error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
