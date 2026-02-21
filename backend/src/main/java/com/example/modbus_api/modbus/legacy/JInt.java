package com.example.modbus_api.modbus.legacy;

public class JInt {
    private int value=0;
    public JInt(int val){
        value = val;
        }
    public int getValue(){ return value; }
    public void setValue(int value) {
        this.value = value;
        }
    public String toString(){ return ""+value; }
    public JInt(){}
}
