package com.example.modbus_api.modbus.legacy;

public class EventPLC extends EventFacade {
    public final static int EventPLC=16;            // Сообщение оборудования
    public EventPLC(){
        super(EventPLC);
    }
    public EventPLC(int level, int action, int source, int accessLevel, String devName, String equipName, int regNum, int bitNum, String mes, String comment){
        super(EventPLC,level,accessLevel);
        ArchESSEvent event = getEvent();
        event.setValue(action);
        event.setPrevValue(source);
        event.setComment(mes+" "+comment);
        OwnDateTime time = new OwnDateTime();
        event.setArrivalTime(time);
        event.setEndTime(time);
        event.setDevName(devName);
        event.setEquipName(equipName);
        event.setRegNum(regNum);
        event.setBitNum(bitNum);
        event.setTitle(toString());
    }
    public EventPLC(int level, int action, int source, int accessLevel, boolean accum,String mes, String comment){
        this(level,action,source,accessLevel,"","",-1,-1,mes,comment);
        getEvent().setSecondRegNum(1);
    }
    public EventPLC(int level, int action, int source, int accessLevel, boolean accum,String devName, String equipName, String mes, String comment){
        this(level,action,source,accessLevel,devName,equipName,-1,-1,mes,comment);
        getEvent().setSecondRegNum(1);
    }
    public EventPLC(int level, int action, int source, int accessLevel, boolean accum,String moduleName, String mes, String comment){
        this(level,action,source,accessLevel,"",moduleName,-1,-1,mes,comment);
        getEvent().setSecondRegNum(1);
    }
    public EventPLC(int level, int action, int source, int accessLevel, String mes, String comment){
        this(level,action,source,accessLevel,"","",-1,-1,mes,comment);
    }
    public EventPLC(int level, int action, int source, int accessLevel, String devName, String equipName, String mes, String comment){
        this(level,action,source,accessLevel,devName,equipName,-1,-1,mes,comment);
    }
    public EventPLC(int level, int action, int source, int accessLevel, String moduleName, String mes, String comment){
        this(level,action,source,accessLevel,"",moduleName,-1,-1,mes,comment);
    }
    public String toString(){
        ArchESSEvent essEvent = getEvent();
        String out = essEvent.getArrivalTime().dateTimeToString();
        out += ", Уровень:"+essEvent.getLevel();
        out += ", Реакция:"+(int)essEvent.getValue();
        out += ", Источник:"+(int)essEvent.getPrevValue();
        out += ", Просмотр:"+essEvent.getViewLevel();
        if (essEvent.getSecondRegNum()!=0)
            out += ", Накопление";
        if (essEvent.getDevName().length()!=0)
            out += ", Контроллер:"+essEvent.getDevName();
        if (essEvent.getEquipName().length()!=0)
            out += ", Оборудование/модуль:"+essEvent.getEquipName();
        if (essEvent.getRegNum()!=-1)
            out += String.format(", Регистр 0x%x(%d)",essEvent.getRegNum(),essEvent.getRegNum());
        if (essEvent.getBitNum()!=-1)
            out += "["+essEvent.getBitNum()+"]";
        out+=", "+essEvent.getComment();
        return out;
    }
    public int getLevel(){ return getEvent().getLevel(); }
    public int getCode(){ return (int)getEvent().getValue(); }
    public String getTitle(){ return getEvent().getTitle(); }
}

