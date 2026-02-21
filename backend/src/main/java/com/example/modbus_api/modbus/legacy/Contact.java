package com.example.modbus_api.modbus.legacy;


public class Contact {
    protected String value="";
    public String getValue() {
        return value;
        }
    public void setValue(String value) {
        this.value = value;
        }
    public Contact(String val){
        value = val;
        }
    public Contact(){
        }
    public String getName() {
        return !valid() ? "???" : value ;
        }
    public String typeName() {
        return "???";
        }
    public int typeId() { return -1; }
    public Class typeClass() { return Object.class; }
    public boolean valid(){ return false; }
    public boolean parseAndSet(String ss){ return false; }
    public String toString(){ return value; }
    public String toStringValue() {
        return value;
        }
    public void parseValue(String ss) throws Exception {
        value = ss;
        }
}
