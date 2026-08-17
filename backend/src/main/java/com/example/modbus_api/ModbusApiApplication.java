package com.example.modbus_api;

import com.example.modbus_api.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class ModbusApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModbusApiApplication.class, args);
    }
}
