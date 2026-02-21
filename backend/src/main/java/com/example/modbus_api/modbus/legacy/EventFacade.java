package com.example.modbus_api.modbus.legacy;

import lombok.Getter;
import lombok.Setter;

public class EventFacade {
    @Getter
    @Setter
    private ArchESSEvent event=new ArchESSEvent(getClass().getName());
    public ArchESSEvent getEvent() {
        return event;
    }
    public EventFacade(){
    }
    public EventFacade(ArchESSEvent essEvent){
        event = essEvent;
    }
    public EventFacade(int type){
        event.setType(type);
    }
    public EventFacade(int type, int level,int viewLevel,String title, String comment){
        event.setType(type);
        event.setLevel(level);
        event.setTitle(title);
        event.setComment(comment);
        event.setViewLevel(viewLevel);
    }
    public EventFacade(int type, int level,int viewLevel){
        event.setType(type);
        event.setLevel(level);
        event.setViewLevel(viewLevel);
    }
    public String toString(){
        return event.toString();
    }
    public EventFacade setLevel(int level){
        event.setLevel(level);
        return this;
    }
    public EventFacade setViewLevel(int level){
        event.setViewLevel(level);
        return this;
    }
    public EventFacade setArtifact(long artId){
        event.getArtifact().setOid(artId);
        return this;
    }
    public EventFacade setArtifact(Artifact art){
        event.getArtifact().setOidRef(art);
        return this;
    }
    // Гость: кроме info и warning, остальные - всё
    public void setDefaultViewLevel(){
        event.setDefaultViewLevel();
    }
}

