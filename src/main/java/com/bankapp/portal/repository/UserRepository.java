package com.bankapp.portal.repository;

import com.bankapp.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithPermissions(@Param("email") String email);

    Optional<User> findByEmail(String email);

    @Query("SELECT r FROM Role r WHERE r.name = :name")
    Optional<com.bankapp.portal.model.Role> findRoleByName(@Param("name") String name);
}