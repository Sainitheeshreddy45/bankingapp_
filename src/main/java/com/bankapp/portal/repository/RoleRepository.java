package com.bankapp.portal.repository;

import com.bankapp.portal.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 🔍 Dynamic Query Finder
     * Extracted by Spring Data to locate system metadata parameters safely.
     */
    Optional<Role> findByName(String name);
}
