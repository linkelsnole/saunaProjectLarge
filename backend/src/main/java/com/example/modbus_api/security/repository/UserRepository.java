package com.example.modbus_api.security.repository;

import com.example.modbus_api.security.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long>{
    Optional<AppUser> findByUsername(String username);
    Boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRole(com.example.modbus_api.security.model.Role role);
}
