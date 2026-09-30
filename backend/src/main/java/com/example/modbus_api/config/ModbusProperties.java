package com.example.modbus_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "modbus.api")
public class ModbusProperties {
    private String logName = "Sauna21000PLC";
    private String ip;
    private String port;
    private String username;
    private String password;
    private int connectTimeout = 30;
    private int readTimeout = 30;
    private int writeTimeout = 20;
		private boolean enableHttpLogging = false;

		public boolean isEnableHttpLogging() {
			return enableHttpLogging;
		}
	
		public void setEnableHttpLogging(boolean enableHttpLogging) {
				this.enableHttpLogging = enableHttpLogging;
		}
    
    public String getLogName() { return logName; }
    public void setLogName(String logName) {
        if (logName == null || !logName.matches("[A-Za-z0-9_.-]{1,64}"))
            throw new IllegalArgumentException("Некорректное имя логического устройства");
        this.logName = logName;
    }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    
    public int getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; }
}