package com.example.modbus_api.modbus.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorList {
    private List<String> errors = new ArrayList<>();

    @SerializedName(value = "info", alternate = {"infos"})
    private List<String> info = new ArrayList<>();
}
