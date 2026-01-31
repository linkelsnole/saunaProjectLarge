package com.example.modbus_api.modbus.model;

import java.io.IOException;

public class UniException extends IOException {
    private final int httpCode;

    public UniException(String message, int httpCode) {
        super(message);
        this.httpCode = httpCode;
    }

    public UniException(String message) {
        super(message);
        this.httpCode = 0;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public static UniException io(String message) {
        return new UniException(message);
    }

    public static UniException io(String message, int code) {
        return new UniException(message, code);
    }

    public static UniException io(Exception ex) {
        return new UniException(ex.getMessage(), 0);
    }
}
