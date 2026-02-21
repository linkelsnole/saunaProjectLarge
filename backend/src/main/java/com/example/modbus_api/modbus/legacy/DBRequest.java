package com.example.modbus_api.modbus.legacy;

import com.google.gson.Gson;
import lombok.Getter;

public class DBRequest {
    private String className="";
    @Getter
    private String jsonObject="";
    public DBRequest(String className, String jsonObject) {
        this.className = className;
        this.jsonObject = jsonObject;
    }
    public DBRequest(){}
    public String getClassName() {
        return className;}
    public DBRequest(Entity ent, Gson gson){
        put(ent,gson);
    }
    public void put(Entity ent, Gson gson){
        className = ent.getClass().getSimpleName();
        jsonObject = gson.toJson(ent);
    }
    public Entity get(Gson gson) throws UniException{
        try {
            Class cc = Class.forName(className);
            if (cc==null)
                throw UniException.bug("Illegal class "+className);
            Entity ent = (Entity) gson.fromJson(jsonObject, cc);
            return ent;
            } catch(Exception ee){
                throw UniException.bug("Illegal object of"+className);
                }
    }
}
