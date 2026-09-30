package com.example.modbus_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleGroupRequest {
    @NotNull private Integer startDate;
    @NotNull private Integer endDate;
    @NotNull @Size(min = 1, max = 7) private List<String> weekDays;
    @Valid @NotEmpty private List<UpdateScheduleIntervalRequest> intervals;
    @JsonProperty("isActive") private boolean active;
}
