package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListData {
    private List<ScheduleData> schedules = new ArrayList<>();
    private String status;
    private String message;
    private Integer totalSlots;
}
