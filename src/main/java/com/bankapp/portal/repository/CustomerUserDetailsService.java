package com.bankapp.portal.repository;

import com.bankapp.portal.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Fetch identity records out of PostgreSQL with all permissions attached
        User user = userRepository.findByEmailWithPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("Access Denied: Account not verified."));

        // Enforce native PCI account blocks (e.g., account lockouts)
        if (!user.isAccountNonLocked()) {
            throw new RuntimeException("Account is temporarily locked due to excessive authentication failures.");
        }

        // Returns your User entity (which implements UserDetails natively) back to Spring's core context
        return user;
    }
}