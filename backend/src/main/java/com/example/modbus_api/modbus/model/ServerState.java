package com.example.modbus_api.modbus.model;

import lombok.Data;

@Data
public class ServerState {
    private boolean serverRun;
    private boolean locked;
    private int requestNum;
    private int sessionCount;
    private long timeSum;
    private long timeMin;
    private long timeMax;
    private long timeCount;
    private long pid;
    private int releaseNumber;
    private String version;
    private int totalGetCount;
    private int cashGetCount;
    private boolean cashEnabled;
}
