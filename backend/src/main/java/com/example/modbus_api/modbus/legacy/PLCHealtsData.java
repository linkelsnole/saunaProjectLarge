package com.example.modbus_api.modbus.legacy;

import lombok.Getter;

import java.util.ArrayList;

public class PLCHealtsData {
    @Getter
    int warningCount=0;
    @Getter int errorCount=0;
    @Getter int failCount=0;
    @Getter
    ArrayList<String> errors = new ArrayList<>();
    public void addInfo(String ss){
        errors.add("info: "+ss);
    }
    public void addWarning(String ss){
        errors.add("warning: "+ss);
        warningCount++;
    }
    public void addAlarm(String ss){
        errors.add("alarm: "+ss);
        errorCount++;
    }
    public void addFatal(String ss){
        errors.add("fatal: "+ss);
        failCount++;
    }
    public boolean noErrors(){
        return errors.size()==0;
    }
    public boolean hasData(){
        return errors.size()!=0;
    }
    public String toString(){
        if (errors.size()==0)
            return "";
        String out = "предупреждений "+warningCount+", сбоев "+errorCount+" аварий "+failCount+" ";
        for(String ss : errors)
            out+='\n'+ss;
        return out;
    }
    public void addData(PLCHealtsData two){
        for(String ss : two.errors)
            errors.add(ss);
        warningCount += two.warningCount;
        errorCount += two.errorCount;
        failCount += two.failCount;
        }
    public final static int ELInfo=0;
    public final static int ELWarning=1;
    public final static int ELError=2;
    public final static int ELFailure=3;
    public final static int ELCrash=4;
    public int healtsLevel(){
        if (failCount!=0)
            return ELFailure;
        if (errorCount!=0)
            return ELError;
        if (warningCount!=0)
            return ELWarning;
        return ELInfo;
    }
}
