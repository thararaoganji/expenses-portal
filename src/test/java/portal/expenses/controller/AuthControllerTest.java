package portal.expenses.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import portal.expenses.config.ApplicationConfig;
import portal.expenses.config.CustomAuthenticationEntryPoint;
import portal.expenses.config.WebSecurityConfig;
import portal.expenses.dto.LoginRequest;
import portal.expenses.repository.UserRepository;
import portal.expenses.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import portal.expenses.util.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class) // Test AuthController
@Import({WebSecurityConfig.class, ApplicationConfig.class}) // Explicitly import your security configurations
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("user", "password");
        UserDetails userDetails = new User("user", "password", new ArrayList<>());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        portal.expenses.entity.AppUser appUser = new portal.expenses.entity.AppUser();
        appUser.setUsername("user");
        appUser.setName("Test User");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("mock-jwt-token");
        when(userRepository.findByUsername("user")).thenReturn(java.util.Optional.of(appUser));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsAreInvalid() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("user", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
