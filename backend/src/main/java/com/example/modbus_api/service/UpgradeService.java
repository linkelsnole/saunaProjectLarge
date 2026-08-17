package com.example.modbus_api.service;

import com.example.modbus_api.modbus.model.ErrorList;
import com.example.modbus_api.modbus.model.ServerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UpgradeService {
    private final ModbusClientAdapter modbusClient;

    @Autowired
    public UpgradeService(ModbusClientAdapter modbusClient) {
        this.modbusClient = modbusClient;
    }

    public ErrorList loadUpgrade(String url, boolean force) throws IOException {
        return modbusClient.upgradeLoad(url, force);
    }

    public ErrorList executeUpgrade(boolean force, boolean self) throws IOException {
        return modbusClient.upgradeExecute(force, self);
    }

    public String readStoredUpgradeUrl() throws IOException {
        return modbusClient.readUpgradeStoredUrl();
    }

    public void writeStoredUpgradeUrl(String url) throws IOException {
        modbusClient.writeUpgradeStoredUrl(url);
    }

    public ServerState getServerState() throws IOException {
        return modbusClient.getServerState();
    }
}
