package com.pharmacy.security;

import com.pharmacy.entity.User;
import com.pharmacy.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Check if user is active
        if (!user.isActive()) {
            throw new UsernameNotFoundException("User is deactivated: " + email);
        }

        // Check if account is locked
        if (user.isLocked()) {
            throw new UsernameNotFoundException("User account is locked: " + email);
        }

        // Convert our User to Spring Security UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),                    // enabled
                true,                               // accountNonExpired
                true,                               // credentialsNonExpired
                !user.isLocked(),                   // accountNonLocked
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }

    // Load user entity by email (for getting full user info)
    public User loadUserEntityByUsername(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
