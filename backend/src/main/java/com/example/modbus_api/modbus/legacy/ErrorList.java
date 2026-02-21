package com.example.modbus_api.modbus.legacy;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class ErrorList {
    @Getter
    private ArrayList<String> errors = new ArrayList<>();
    private ArrayList<String> info = new ArrayList<>();
    @Getter
    private long duration=0;            // Продолжительность в МС
    private transient long startTime=0;
    public void setDuration(){
        startTime = new OwnDateTime().timeInMS();
        }
    public void calcDuration(){
        duration = new OwnDateTime().timeInMS()-startTime;
        }
    public ErrorList addError(String ss){
        errors.add(ss);
        return this;
        }
    public ErrorList(){
        setDuration();
        }
    public ErrorList(String ss){
        info.add(ss);
        setDuration();
        }
    public void clear(){
        errors.clear();
        info.clear();
        }
    public boolean valid(){ return errors.size()==0; }
    public boolean isEmpty(){ return errors.size()==0 && info.size()==0; }
    public ErrorList addError(ErrorList two){
        for(String ss : two.errors)
            errors.add(ss);
        for(String ss : two.info)
            info.add(ss);
        return this;
        }
    public ErrorList addInfo(String ss){
        info.add(ss);
        return this;
        }
    //public String getInfo(){
    //     return toString();
    //     }
    public String toString(){
        String ss="";
        for(String vv : info)
            ss+=vv+"\n";
        if (errors.size()!=0)
            ss+="Ошибок: "+errors.size()+"\n";
        for(String vv : errors)
            ss+=vv+"\n";
        if (duration!=0)
            ss+="Продолжительность " + duration + " мс";
        return ss;
        }
    public int getErrCount(){
        return errors.size();
        }
    }

