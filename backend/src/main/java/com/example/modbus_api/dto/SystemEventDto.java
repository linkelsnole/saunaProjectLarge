package com.example.modbus_api.dto;

import lombok.Data;

/** Read-only wire model. Missing upstream fields stay null, including level and timestamps. */
@Data
public class SystemEventDto {
    private String oid;
    private Boolean valid;
    private Integer type;
    private Integer level;
    private String title;
    private String comment;
    private EventTime arrivalTime;
    private EventTime endTime;
    private String equipName;
    private String devName;
    private Integer regNum;
    private Integer bitNum;
    private Double prevValue;
    private Double value;
    private Integer secondRegNum;
    private Double secondRegValue;
    private Integer viewLevel;
    private String packedValues;
    private Integer logUnit;
    private String facadeClass;

    @Data
    public static class EventTime {
        private Long timeInMS;
    }
}
