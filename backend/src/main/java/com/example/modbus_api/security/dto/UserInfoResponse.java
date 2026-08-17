package com.example.modbus_api.security.dto;

import com.example.modbus_api.security.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String role;

    public static UserInfoResponse from(AppUser user) {
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getRole().name());
    }
}
