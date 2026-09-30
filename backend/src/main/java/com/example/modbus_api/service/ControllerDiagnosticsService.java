package com.example.modbus_api.service;

import com.example.modbus_api.config.ModbusProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Service
public class ControllerDiagnosticsService {
    // Air2Common: State, WorkState, ModbusErrors, ErrRestarts, FailRestarts.
    private static final java.util.List<Integer> REGISTERS = Arrays.asList(0x0301,0x0305,0x0600,0x0604,0x0605);
    private final ModbusClientAdapter client;
    private final ModbusProperties config;
    public ControllerDiagnosticsService(ModbusClientAdapter client,ModbusProperties config) { this.client=client;this.config=config; }

    public Snapshot read() throws IOException {
        Map<Integer,Integer> values=client.readRegisters(ModbusConstants.SAUNA_PLC,REGISTERS);
        if(values==null) throw new IOException("Empty register response");
        // 0x8000 is a valid WorkState value: bit 15 is EvtCollapse. Never replace it with zero/null.
        return new Snapshot(config.getLogName(),System.currentTimeMillis(),word(values.get(0x0301)),word(values.get(0x0305)),
            word(values.get(0x0600)),word(values.get(0x0604)),word(values.get(0x0605)));
    }
    private Integer word(Integer value) { return value==null || value<0 || value>65535 ? null:value; }

    @Data @AllArgsConstructor
    public static class Snapshot {
        private String logName;
        private long readAt;
        private Integer state;
        private Integer workState;
        private Integer modbusErrors;
        private Integer errorRestarts;
        private Integer failureRestarts;
    }
}
