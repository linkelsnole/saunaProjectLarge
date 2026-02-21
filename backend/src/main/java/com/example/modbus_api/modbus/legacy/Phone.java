package com.example.modbus_api.modbus.legacy;

public class Phone extends Contact {
    public Phone(String val) {
        parseAndSet(val);
        }
    public Phone(){}
    @Override
    public String getName() {
        return !valid() ? "" : value;
        }
    @Override
    public String typeName() {
        return "Телефон";
        }
    private static String testMobile(String ss){
        if (ss.startsWith("8") && ss.length()==11 && Utils.hasDigits(ss))
            return ss.substring(1);
        if (ss.startsWith("+7") && ss.length()==12 && Utils.hasDigits(ss.substring(1)))
            return ss.substring(2);
        if (ss.length()==10 && Utils.hasDigits(ss))
            return ss;
        return null;
        }
    private static boolean testLocal(String ss){
        return ss.length()==7 && Utils.hasDigits(ss);
        }
    private static boolean valid(String ss){
        if (ss.length()==0) return false;
        if (testMobile(ss)!=null)
            return true;
        if (testLocal(ss))
            return true;
        return false;
        }
    @Override
    public boolean valid() {
        return valid(value);
        }
    @Override
    public boolean parseAndSet(String ss) {
        String zz = testMobile(ss);
        if (zz!=null){
            value=zz;
            return true;
            }
        if (testLocal(ss)){
            value=ss;
            return true;
            }
        return false;
        }
    public boolean isMobile(){
        return testMobile(value)!=null;
        }
    public String mobile(){
        if (!isMobile()) return "";
        if (value.startsWith("8"))
            return value.substring(1);
        if (value.startsWith("+7"))
            return value.substring(2);
        return value;
        }
    public String toString(){
        return valid() ? value : "";
    }
}
