package com.example.modbus_api.modbus.legacy;

//------------------------oid не сохраняется, наслежлование фиктивное --------------------
public class Account extends Entity {
    private String login="";
    private Phone loginPhone = new Phone();
    private String password="";
    public Account(String login, String loginPhone, String password) {
        this.login = login;
        this.loginPhone.parseAndSet(loginPhone);
        this.password = password;
        }
    public Account(){}
    public Phone getPhone(){ return  loginPhone; }
    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getLoginPhone(){ return !loginPhone.isMobile() ? "" : loginPhone.mobile(); }
    public boolean loginPhoneValid(){ return loginPhone.isMobile(); }
    public void  setLoginPhone(String ss){ loginPhone.parseAndSet(ss); }
    public void setLoginPhone(Phone loginPhone) {
        this.loginPhone = loginPhone;
        }
    public String toString(){ return login+" "+loginPhone+" "+password; }
    public String getTitle(){ return login+" / "+loginPhone; }
    public void load(Account proto){
        password = proto.password;
        login = proto.getLogin();
        loginPhone = new Phone(proto.getLoginPhone().toString());
        }
    public Account(Account proto){
        password = proto.getPassword();
        login = proto.getLogin();
        loginPhone = new Phone(proto.getLoginPhone().toString());
    }
    //------------------------------------------------------------------------------------------------------------------
    /*
    @Override
    public void putData(String prefix, org.bson.Document document, int level, I_MongoDB mongo) throws UniException {
        putDBValues(prefix,document,level,mongo);
        loginPhone.putData(prefix,document,0,null);
    }
    @Override
    public void getData(String prefix, org.bson.Document res, int level, I_MongoDB mongo) throws UniException {
        getDBValues(prefix,res);
        loginPhone.getData(prefix,res, 0, null);
        }
    //----------------- Импорт/экспорт Excel ------------------------------------------------------------
    @Override
    public void getData(Row row, ExCellCounter cnt) throws UniException{
        getXMLValues(row, cnt);
        loginPhone.getData(row, cnt);
        }
    @Override
    public void putData(Row row, ExCellCounter cnt) throws UniException{
        putXMLValues(row, cnt);
        loginPhone.putData(row, cnt);
        }
    @Override
    public void putHeader(String prefix, ArrayList<String> list) throws UniException{
        putXMLHeader(prefix,list);
        loginPhone.putHeader(prefix,list);
        }
    */
    //--------------------------------------------------------------------------------------------------
    public String toStringValue() {
        return ""+loginPhone.toStringValue()+"|"+login+"|"+password;
    }
    public void parseValue(String ss) throws Exception {
        int idx1=ss.indexOf("|");
        int idx2=ss.lastIndexOf("|");
        loginPhone=new Phone();
        if (idx1!=0)
            loginPhone=new Phone(ss.substring(0,idx1));
        login="";
        login = ss.substring(idx1+1,idx2);
        password="";
        if (idx2!=ss.length()-1)
            password = ss.substring(idx2+1);
    }
}
