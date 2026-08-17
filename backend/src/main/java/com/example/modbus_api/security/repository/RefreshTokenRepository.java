package com.example.modbus_api.security.repository;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(AppUser user);

    void deleteByToken(String token);
}
