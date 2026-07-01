package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    // --- Module 5: Password History (Tracks last 4 hashes to prevent reuse) ---
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_password_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "historic_password_hash")
    @Builder.Default
    private List<String> passwordHistory = new ArrayList<>();

    // --- Module 5: Account Status & Brute-Force Tracking ---
    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "lockout_end_time")
    private LocalDateTime lockoutEndTime;

    // --- Module 4: Role-Based Access Control ---
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // --- Spring Security UserDetails Interface Implementation ---

    /**
     * Module 4 Requirement: Resource-level permissions must be mapped cleanly
     * straight into Spring Security's authority context.
     */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Add the structural Roles (e.g., "ROLE_SUPER_ADMIN")
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));

        // 2. Add the fine-grained Permissions (e.g., "merchant:approve")
        roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

        return authorities;
    }


    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email; // Email acts as our username identity pointer
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Set to true for stateless session handling
    }

    /**
     * Resolves automated time-based unlocks if the 15-minute penalty window has passed.
     */
    @Override
    public boolean isAccountNonLocked() {
        if (!this.accountNonLocked && this.lockoutEndTime != null) {
            if (this.lockoutEndTime.isBefore(LocalDateTime.now())) {
                this.accountNonLocked = true;
                this.failedLoginAttempts = 0;
                this.lockoutEndTime = null;
            }
        }
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}