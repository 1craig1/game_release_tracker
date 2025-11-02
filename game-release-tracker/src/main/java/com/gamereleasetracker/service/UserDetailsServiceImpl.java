package com.gamereleasetracker.service;

import com.gamereleasetracker.model.User;
import com.gamereleasetracker.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
/**
 * Service to integrate Spring Security with our User data model.
 * This class implements the UserDetailsService interface, which is a core component
 * of Spring Security's authentication mechanism. Its primary responsibility is to load a user's
 * core data (username, password, authorities) from the persistent storage (in this case,
 * the PostgreSQL database via UserRepository) and provide it to the security framework.
 */
@Primary // Tells Spring to use this class as the primary UserDetailsService implementation
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     *  This method is called by the authentication provider to verify a user's
     *  credentials during the login process. It fetches our User entity and
     *  maps it to a UserDetails object, which Spring Security can then use to perform
     *  the password comparison and establish an authenticated session.
     * @param username the username identifying the user whose data is required.
     * @return a UserDetails object containing the user's data.
     * @throws UsernameNotFoundException if the user is not found in the database.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find the user by their username using our repository method
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Get the authorities (roles) for the user
        // We fetch the role name and create a SimpleGrantedAuthority from it
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName().name()));

        // Create and return a Spring Security User object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isAccountNonLocked(),
                authorities);
    }
}
