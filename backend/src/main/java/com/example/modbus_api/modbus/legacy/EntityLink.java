package com.example.modbus_api.modbus.legacy;

/** Ссылка, совмещенная с Id-ом*/
public class EntityLink<T extends Entity>{
    private long oid=0;
    private T ref=null;
    private int operation= 0;                           // Операция - в БД не пишется, используется при add/update/delete
    private transient Class typeT=null;                 // Класс параметра шаблона для рефлексионной загрузки ссылок
    public EntityLink(){}
    public EntityLink(Class type0){
        typeT = type0; }
    public EntityLink(T vv){
        ref=vv;}
    public EntityLink(long id){
        oid=id; }
    public EntityLink(long id,T ref0){
        oid=id; ref = ref0; }
    public EntityLink(T vv, int oper){
        ref=vv; operation=oper; }
    public EntityLink(long id, int oper){
        oid=id; operation=oper; }
    public EntityLink(long id,T ref0, int oper){
        oid=id; ref = ref0; operation=oper; }
    public void setOidRef(long id, T ref0){
        oid = id;
        ref = ref0;
        }
    public void clear(){
        ref=null;
        oid=0;
        }
    public void setOidRef(T ref0){
        if (ref0==null){
            oid=0;
            ref=null;
            }
        else{
            oid = ref0.getOid();
            ref = ref0;
            }
        }
    public void setOidByRef(){
        if (ref!=null)
            oid = ref.getOid();
        }
    //------------------------------------------------------------------------------------------------------------------
    public Class getTypeT() {
        return typeT; }
    public void setTypeT(Class typeT) {
        this.typeT = typeT; }
    public long getOid() {
        return oid; }
    public void setOid(long oid) {
        this.oid = oid;
        }
    public T getRef() { return ref; }
    public void setRef(T ref) { this.ref = ref; }
    public String toShortString(){
        return ref==null ? "" : ref.toShortString(); }
    public String toFullString(){
        if (oid==0) return "";
        return ref==null ? ""+oid+"=???" : ref.toFullString(); }
    public String getTitle(){
        return ref==null ? "...": ref.getTitle(); }
    public int getOperation() {
        return operation; }
    public void setOperation(int operation) {
        this.operation = operation; }
    public void setRefArtifact(Artifact zz) {ref=(T)zz;}            // Для EntityLinkList
    //-------------------------------------------------------------------------------------
    public static void main(String a[]){
        /*
        EntityLink<Document> ent = new EntityLink<>();
        EntityLink<MaintenanceJob> ent2 = new EntityLink<>();
        Class cc = ent.getClass();
        Class cc2 = ent2.getClass();
        System.out.println(cc==cc2);
         */
    }
}
