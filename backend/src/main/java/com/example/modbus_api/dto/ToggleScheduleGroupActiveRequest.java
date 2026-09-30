package com.example.modbus_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleScheduleGroupActiveRequest {
    @JsonProperty("isActive") private boolean active;
}
