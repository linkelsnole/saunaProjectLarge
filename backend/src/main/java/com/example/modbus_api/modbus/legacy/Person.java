package com.example.modbus_api.modbus.legacy;


public class Person extends Entity {
    private String lastName="";                 //Фамилия
    private String firstName="";                // Имя
    private String middleName="";               // Отчество
    private Phone phone = new Phone();
    private Mail mail = new Mail();
    private String post="";                     // Должность
    //------------------------------------------------------------------------------------------------------------------
    /*
    @Override
    public void putData(String prefix, org.bson.Document document, int level, I_MongoDB mongo) throws UniException {
        putDBValues(prefix,document,level,mongo);
        phone.putData("p_",document,0,null);
        mail.putData("m_",document,0,null);
    }
    @Override
    public void getData(String prefix, org.bson.Document res, int level, I_MongoDB mongo) throws UniException {
        getDBValues(prefix,res,level,mongo);
        phone.getData("p_",res, 0, null);
        mail.getData("m_",res, 0, null);
        }
    //----------------- Импорт/экспорт Excel ------------------------------------------------------------
    @Override
    public void getData(Row row, ExCellCounter cnt) throws UniException{
        getXMLValues(row, cnt);
        phone.getData(row, cnt);
        mail.getData(row, cnt);
        }
    @Override
    public void putData(Row row, ExCellCounter cnt) throws UniException{
        putXMLValues(row, cnt);
        phone.putData(row, cnt);
        mail.putData(row, cnt);
        }
    @Override
    public void putHeader(String prefix, ArrayList<String> list) throws UniException{
        putXMLHeader(prefix,list);
        phone.putHeader("p_",list);
        mail.putHeader("m_",list);
        }
    */
    //-----------------------------------------------------------------------------------------------------
    public Person(){}
    public Person(String nm1, String nm2, String nm3,String post0,String phone0, String mail0){
        this(nm1,nm2,nm3);
        post = post0;
        phone = new Phone(phone0);
        mail = new Mail(mail0);
        }
    public Person(String nm1, String nm2, String nm3){
        lastName=nm1;
        firstName=nm2;
        middleName=nm3;
        }
    public boolean valid(){ return lastName.length()!=0; }
    public String fullUserName(){
        String out = lastName;
        if (firstName.length()==0) return out;
        out+=" "+firstName;
        if (middleName.length()==0) return out;
        out+=" "+middleName;
        return out;
        }
    public String fullUserNameWhen(){
        String out = Utils.when(lastName);
        if (firstName.length()==0) return out;
        out+=" "+Utils.when(firstName);
        if (middleName.length()==0) return out;
        out+=" "+Utils.when(middleName);
        return out;
    }
    @Override
    public String getTitle() {
        return shortUserName()+","+post + ","+phone.toString()+","+mail.toString(); }
    public String getPersonTitle() {
        return shortUserName()+","+post + ","+phone.toString()+","+mail.toString(); }
    public String toString(){
        return lastName+" "+firstName+" "+middleName + " ["+post+","+phone+","+mail+"] ";
        }
    public String toFullString(){
        return super.toFullString()+lastName+" "+firstName+" "+middleName + " ["+post+","+phone+","+mail+"] ";
    }
    public String shortUserName(){
        if (lastName.length()==0)
            return "...";
        String out = lastName;
        if (firstName.length()==0) return out;
        out+=" "+firstName.substring(0,1)+".";
        if (middleName.length()==0) return out;
        out+=" "+middleName.substring(0,1)+".";
        return out;
        }
    public void load(Person proto){
        firstName = proto.firstName;
        lastName = proto.lastName;
        middleName = proto.middleName;
        phone = new Phone(proto.phone.toString());
        mail = new Mail(proto.mail.toString());
        post = proto.post;
        }
    public String objectName() {
        return shortUserName(); }
    public String getLastName() {
        return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName; }
    public String getFirstName() {
        return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName; }
    public String getMiddleName() {
        return middleName; }
    public void setMiddleName(String middleName) {
        this.middleName = middleName; }
    public Phone getPhone() {
        return phone; }
    public void setPhone(Phone phone) {
        this.phone = phone; }
    public Mail getMail() {
        return mail; }
    public void setMail(Mail mail) {
        this.mail = mail; }
    public String getPost() {
        return post; }
    public void setPost(String post) {
        this.post = post; }
}
