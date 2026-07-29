package portal.expenses.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private static final String ROLE_EMPLOYEE = String.valueOf("ROLE_EMPLOYEE");
    private static final String ROLE_MANAGER = String.valueOf("ROLE_MANAGER");
    private static final String ROLE_FINANCE = String.valueOf("ROLE_FINANCE");

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final AuthenticationProvider authenticationProvider;

    public WebSecurityConfig(JwtAuthFilter jwtAuthFilter,
                             CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                             AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Actuator endpoints: completely bypass JWT and entry point.
     */
    @Bean
    @Order(0)
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Replace with your actual CloudFront/S3 domain
        configuration.setAllowedOrigins(Arrays.asList("https://du46cvjytrhz9.cloudfront.net","http://localhost:4200",
                "http://localhost:3000"));

        // Explicitly allow methods used by your Angular app
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // Allow all headers - this is important for CORS preflight
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow the browser to read the Authorization header if needed
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        configuration.setAllowCredentials(true);

        // Set max age for preflight cache (in seconds)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Application endpoints: protected with JWT.
     */
    @Bean
    @Order(1)
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users").hasAnyAuthority(ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_FINANCE)
                        .requestMatchers(HttpMethod.POST, "/users").hasAuthority(ROLE_MANAGER)
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasAuthority(ROLE_FINANCE)
                        .requestMatchers("/expenses/**").hasAnyAuthority(ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_FINANCE)
                        .requestMatchers("/manager/**").hasAuthority(ROLE_MANAGER)
                        .requestMatchers("/finance/**").hasAuthority(ROLE_FINANCE)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
