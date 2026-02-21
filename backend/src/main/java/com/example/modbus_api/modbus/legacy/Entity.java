package com.example.modbus_api.modbus.legacy;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;

public class Entity {
    private long oid=0;
    private boolean valid=true;
    public String toFullString(){ return "["+oid+"] "; }
    public String toString(){ return "["+oid+"] "; }
    public String toShortString(){ return ""; }
    public String getTitle(){ return toString(); }
    public long getOid() {
        return oid;
        }
    public void setOid(long oid) {
        this.oid = oid;
        }
    public Entity(long id0){ oid=id0; }
    public Entity(){ oid=0; }
    public int getTitleLevel(){ return 0; }
    public String toJSON(){
        Gson gs = new Gson();
        return gs.toJson(this);
        }
    public boolean isValid(){ return valid; }
    public void setValid(boolean bb){ valid=bb; }
}
