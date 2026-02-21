package com.example.modbus_api.modbus.legacy;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class UnitRegisterList{
    @Getter @Setter private int unitIdx=0;
    @Getter @Setter private ArrayList<Integer> data = new ArrayList<>();
    public int size(){ return data.size(); }
    public Integer get(int idx){ return data.get(idx);}
    public void add(Integer vv){ data.add(vv); }
}
