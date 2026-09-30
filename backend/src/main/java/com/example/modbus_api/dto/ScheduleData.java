package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleData {
    private int scheduleId;
    private ScheduleMode mode;
    private ScheduleStatus status;
    private int statusRawCode;
    private int weekDayMask;
    private Integer group;
    private DateRange dateRange;
    private TimeRange timeRange;
    private int duration;
    private AirControl airControl;
    private TemperatureMode temperatureModeZone1;
    private TemperatureZoneControl temperatureZone1;
    private TemperatureMode temperatureModeZone2;
    private TemperatureZoneControl temperatureZone2;

    public ScheduleData(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public enum ScheduleMode {
        NONE(0), DEFAULT(1), NIGHT(2), DAYS_OF_WEEK(3), DATES(4), USERSET(5);
        private final int code;
        ScheduleMode(int code) { this.code = code; }
        public int getCode() { return code; }
        public static ScheduleMode fromCode(int code) {
            for (ScheduleMode value : values()) if (value.code == code) return value;
            return NONE;
        }
    }

    public enum ScheduleStatus { INACTIVE, ACTIVE }

    public enum VentilationMode {
        DISABLED(0), ECO(1), NORMAL(2), INTENSIVE(3), MANUAL(4);
        private final int code;
        VentilationMode(int code) { this.code = code; }
        public int getCode() { return code; }
        public static VentilationMode fromCode(int code) {
            for (VentilationMode value : values()) if (value.code == code) return value;
            return DISABLED;
        }
    }

    public enum TemperatureMode {
        DISABLED(0), ECO(1), AUTO(2), MANUAL(3);
        private final int code;
        TemperatureMode(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TemperatureMode fromCode(int code) {
            for (TemperatureMode value : values()) if (value.code == code) return value;
            return DISABLED;
        }
    }

    public enum TemperatureControlType {
        ORP(0), PV(1), OV(2);
        private final int code;
        TemperatureControlType(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TemperatureControlType fromCode(int code) {
            for (TemperatureControlType value : values()) if (value.code == code) return value;
            return ORP;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DateRange { private int startDate; private int endDate; }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TimeRange { private int startTime; private int endTime; }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AirControl { private VentilationMode mode; private Double targetAirflow; }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TemperatureZoneControl {
        private TemperatureControlType controlType;
        private Double targetTemperature;
    }
}
