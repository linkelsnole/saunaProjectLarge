package com.example.modbus_api.modbus.legacy;

import com.google.gson.Gson;


import java.util.ArrayList;

public class EntityList<T extends Entity> extends ArrayList<T> {
    public EntityList(){}
    public Entity getById(long userId){
        for (Entity uu : this)
            if (uu.getOid()==userId)
                return uu;
        return null;
        }
    public int getIdxById(long userId){
        int idx=0;
        for (Entity uu : this){
            if (uu.getOid()==userId)
                return idx;
            idx++;
            }
        return -1;
    }
    public EntityList(EntityList<Entity> src){
        for(Entity ent : src)
            add((T)ent);
        }
    public ArrayList<Entity> convert(){
        ArrayList<Entity> out = new ArrayList<>();
        for (Entity uu : this)
            out.add(uu);
        return out;
        }
    public void add(EntityList<T> two){
        for(T ent : two)
            add(ent);
        }
    public String toString(){
        String out="";
        for(int i=0;i<size(); i++){
            Entity uu = get(i);
            if (i!=0) out+="\n";
            out+=uu.getTitle();
            }
        return out;
        }
    public String toShortString(){
        String out="";
        for(int i=0;i<size(); i++){
            Entity uu = get(i);
            if (i!=0) out+="\n";
            out+=uu.toShortString();
            }
        return out;
        }
    public String toFullString(){
        String out="";
        for(int i=0;i<size(); i++){
            Entity uu = get(i);
            if (i!=0) out+="\n";
            out+=uu.toFullString();
        }
        return out;
        }
    }
