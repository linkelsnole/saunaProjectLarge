package com.example.modbus_api.modbus.legacy;

public class EntityBack extends Entity{
    private String parentName="";
    private long parentOid=0;
    public EntityBack(){}
    public EntityBack(String nm, long id){
        parentName=nm;
        parentOid=id;
        }
    public EntityBack(Entity parent){
        parentName = parent.getClass().getSimpleName();
        parentOid = parent.getOid();
        }
    public String getParentName() {
        return parentName; }
    public void setParentName(String parentName) {
        this.parentName = parentName; }
    public long getParentOid() {
        return parentOid; }
    public void setParentOid(long parentOid) {
        this.parentOid = parentOid; }
    public void setParent(Entity parent){
        parentName = parent.getClass().getSimpleName();
        parentOid = parent.getOid();
        }

    @Override
    public String toFullString() {
        return super.toFullString()+ (parentName.length()!=0 ? parentName+"["+parentOid+"] " : "");
    }
}
