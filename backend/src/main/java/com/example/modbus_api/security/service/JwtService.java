package com.example.modbus_api.security.service;

import com.example.modbus_api.config.JwtProperties;
import com.example.modbus_api.service.DefaultCredentialsRotation;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final com.example.modbus_api.service.DeletedUsers deleted;
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties,com.example.modbus_api.service.DeletedUsers deleted) {
        this.deleted=deleted;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<String, Object>();
        Long lastDeletion=deleted.deletedAt(userDetails.getUsername());
        claims.put("sessionCreatedAt", Math.max(System.currentTimeMillis(),lastDeletion==null?0:lastDeletion+1));
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        if (DefaultCredentialsRotation.appliesTo(userDetails.getUsername())) claims.put("credentialsVersion", DefaultCredentialsRotation.VERSION);
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtProperties.getAccessTokenExpirationMs());

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        Long deletedAt=deleted.deletedAt(username);
        Number created=(Number)claims.get("sessionCreatedAt");
        long issued=created!=null?created.longValue():claims.getIssuedAt()==null?0:claims.getIssuedAt().getTime();
        if(deletedAt!=null && issued<=deletedAt) return false;
        if (DefaultCredentialsRotation.appliesTo(username) && !DefaultCredentialsRotation.VERSION.equals(claims.get("credentialsVersion", String.class))) return false;
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
