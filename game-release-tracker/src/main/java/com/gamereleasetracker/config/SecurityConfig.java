package com.gamereleasetracker.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SecurityConfig is the main class for configuring the security settings of the application.
 * It uses Spring Security to define authentication mechanisms, access control, CORS settings,
 * and custom behavior for endpoints. This class integrates Spring Security features such
 * as Remember-Me authentication, CSRF protection, and CORS configuration.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final PersistentTokenRepository persistentTokenRepository;

    // Spring will automatically find our implementations of these interfaces and inject them
    public SecurityConfig(UserDetailsService userDetailsService, PersistentTokenRepository persistentTokenRepository) {
        this.userDetailsService = userDetailsService;
        this.persistentTokenRepository = persistentTokenRepository;
    }

    /**
     * Configures the security filter chain for the application, defining security rules such as CORS, CSRF protection,
     * request authorization, logout behavior, and exception handling. This method integrates custom RememberMeServices
     * and SPA-specific CSRF handling to enhance security and usability.
     *
     * @param http the HttpSecurity object used to configure various security settings such as authorization,
     *             authentication, CORS, CSRF, and exception handling.
     * @param rememberMeServices the service responsible for managing "remember-me" authentication, enabling
     *                           automatic login through browser cookies.
     * @return the SecurityFilterChain instance representing the configured security filters.
     * @throws Exception if there is a problem during the configuration or building of the SecurityFilterChain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RememberMeServices rememberMeServices) throws Exception {
        http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Enable CSRF protection
                .csrf(csrf -> csrf
                        // Disable the default CSRF cookie, which is sent to the client in a header.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Enable the custom CSRF token handler for Single-Page Applications (SPAs).
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        // Allow login and register to bypass CSRF protection.
                        .ignoringRequestMatchers("/api/auth/login", "/api/auth/register")
                )
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to these specific endpoints.
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/error").permitAll()
                        // For ANY OTHER request that wasn't matched above, require authentication.
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        // Set the logout URL to "/api/auth/logout"
                        .logoutUrl("/api/auth/logout")
                        // Set the logout success handler to return a 200 OK status.
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value()))
                        // Delete the session, remember-me cookies, and CSRF token.
                        .deleteCookies("SESSION", "remember-me", "XSRF-TOKEN")
                        // Invalidate the session
                        .invalidateHttpSession(true)
                )
                // Integrate remember-me services with the application.
                .rememberMe(remember -> remember.rememberMeServices(rememberMeServices))
                // Handle unauthorized requests by returning a 401 Unauthorized status.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }

    /**
     * Creates RememberMeServices as a separate, configurable bean.
     * This follows the pattern from Spring Security documentation for a cleaner setup.
     * We are using PersistentTokenBasedRememberMeServices, which is the concrete
     * implementation for the secure, database-backed approach.
     */
    @Bean
    public RememberMeServices rememberMeServices() {
        // A unique key for this application instance. Using a random UUID is a good practice.
        String key = UUID.randomUUID().toString();

        PersistentTokenBasedRememberMeServices rememberMeServices =
                new PersistentTokenBasedRememberMeServices(key, userDetailsService, persistentTokenRepository);

        // Configure the service properties
        rememberMeServices.setTokenValiditySeconds(60 * 60 * 24 * 14); // 14 days
        // This tells the service to bypass its internal check for a 'remember-me' parameter.
        // It will now rely entirely on AuthController's control flow.
        rememberMeServices.setAlwaysRemember(true);
        return rememberMeServices;
    }


    /**
     * Provides an AuthenticationManager bean for managing authentication processes
     * within the application. This method retrieves the AuthenticationManager from the
     * specified AuthenticationConfiguration.
     *
     * @param authenticationConfiguration the configuration object used to retrieve the
     *                                     AuthenticationManager, containing authentication settings.
     * @return the AuthenticationManager instance responsible for handling authentication.
     * @throws Exception if an error occurs while retrieving the AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * Configures a CorsConfigurationSource to define Cross-Origin Resource Sharing (CORS) settings for the application.
     * This configuration allows specific origins, HTTP methods, and headers while enabling credentials sharing.
     *
     * @return the configured CorsConfigurationSource instance that manages CORS rules for incoming requests.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Define the allowed CORS origins, HTTP methods, and headers.
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // Apply the configuration to all endpoints.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

/**
 * FROM: <a href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa">
 *     Spring Security CSRF Documentation </a>
 * A custom CsrfTokenRequestHandler that is optimized for Single-Page Applications (SPAs).
 * This handler ensures that a new CSRF token is sent to the client after login and logout,
 * which is crucial for SPAs that rely on the XSRF-TOKEN cookie.
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        /*
         * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
         * the CsrfToken when it is rendered in the response body.
         */
        this.xor.handle(request, response, csrfToken);
        /*
         * Render the token value to a cookie by causing the deferred token to be loaded.
         * This is necessary to ensure the client receives a new token after login/logout.
         */
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        /*
         * If the request contains a request header (e.g., X-XSRF-TOKEN), use the plain
         * CsrfTokenRequestAttributeHandler to resolve the token. This is the standard
         * for SPAs.
         */
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
