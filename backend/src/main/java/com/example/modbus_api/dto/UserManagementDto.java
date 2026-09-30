package com.example.modbus_api.dto;
import com.example.modbus_api.security.model.Role;
import lombok.*;
import javax.validation.constraints.*;
public final class UserManagementDto {
    private UserManagementDto() {}
    @Data @AllArgsConstructor public static class UserView { private Long id; private String username; private Role role; }
    @Data @NoArgsConstructor @AllArgsConstructor public static class Create {
        @NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{3,32}") private String username;
        @NotNull @Size(min=12,max=72) @ToString.Exclude private String password;
        @NotNull private Role role;
    }
    @Data @NoArgsConstructor @AllArgsConstructor public static class ChangePassword {
        @NotNull @Size(min=12,max=72) @ToString.Exclude private String password;
    }
    @Data @NoArgsConstructor @AllArgsConstructor public static class ChangeRole {
        @NotNull private Role role;
        @NotNull private Role expectedRole;
    }
}
