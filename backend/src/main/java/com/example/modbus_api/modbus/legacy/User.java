package com.example.modbus_api.modbus.legacy;


public class User extends Person {
    private int typeId= 0;
    //private Account account=null;
    private EntityLink<Account>  accountData = new EntityLink<>();          // Загружается по API
    private EntityLink<Artifact> photo=new EntityLink<>(Artifact.class);    // Фотография
    private long secondTableId=0;                                           // ID для производного класса
    private String sessionToken="";
    private String simCardICC="";
    //--------------------------------------------------------------------------------------------------
    public User(){
        this(0,"","","","","","");
    }
    public String typeName() {
        return "";
        }
    public User(String log, String loginPh, String pass){
        this(0,"","","",log,pass,loginPh);
        }
    public User(int typeId0, String nm1, String nm2, String nm3, String log, String pass,String loginPh){
        super(nm1,nm2,nm3);
        typeId = typeId0;
        setPhone(new Phone(loginPh));
        //account = new Account(log,loginPh,pass);
        accountData.setRef(new Account(log,loginPh,pass));
        }
    public Account getAccount(){
        return accountData.getRef()!=null ? accountData.getRef() : new Account();   //638
        //return account;   //637
        }
    public void load(User proto){
        setOid(proto.getOid());
        //account = new Account(proto.getAccount());
        accountData.setRef(new Account(proto.getAccount()));
        setLastName(proto.getLastName());
        setFirstName(proto.getFirstName());
        setMiddleName(proto.getMiddleName());
        }
    public String getTitle() {
        return shortUserName();
        }
    public EntityLink<Artifact >getPhoto() {
        return photo; }
    public void setPhoto(EntityLink<Artifact> photo0) {
        photo=photo0; }
    public String toString(){
        return typeName()+" "+super.toString()+photo+" "+getAccount(); }
    public String toFullString(){
        return super.toFullString()+typeName()+" "+super.toString()+getAccount()+" "+photo; }
    public String getLogin(){
        return getAccount().getLogin(); }
    public String getLoginPhone(){
        return getAccount().getLoginPhone(); }
    public boolean loginPhoneValid(){
        return getAccount().loginPhoneValid(); }
    public int getTypeId() {
        return typeId; }
    public void setTypeId(int typeId) {
        this.typeId = typeId; }
    public void setAccount(Account account0) {
        //this.account = account0;
        accountData.setRef(new Account(account0));
        }
    public String getSimCardICC() {
        return simCardICC; }
    public void setSimCardICC(String simCardICC) {
        this.simCardICC = simCardICC; }
    public void setPhoto(Artifact photo0) {
        this.photo.setRef(photo0); }
    public long getSecondTableId() {
        return secondTableId; }
    public void setSecondTableId(long secondTableId) {
        this.secondTableId = secondTableId; }
    public String getSessionToken() {
        return sessionToken; }
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken; }
    public String getPassword(){ return getAccount().getPassword(); }
    public void setPassword(String ss){ getAccount().setPassword(ss); }
    public void setLoginPhone(String ss){ getAccount().setLoginPhone(ss);}
    public void setLogin (String ss){ getAccount().setLogin(ss);}
    public EntityLink<Account> getAccountData() {
        return accountData; }
}
