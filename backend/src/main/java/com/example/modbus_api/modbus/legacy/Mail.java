package com.example.modbus_api.modbus.legacy;

public class Mail extends Contact {
    @Override
    public String typeName() {
        return "E-mail";
    }
    @Override
    public boolean valid() {
        return valid(value);
        }
    public Mail(){}
    public Mail(String ss){
        super(ss);
        }
    public static boolean valid(String ss) {
        char c[] = ss.toCharArray();
        if (c.length==0) return false;
        int cnt=0;
        for(int i=0;i<c.length;i++){
            char cc = c[i];
            if (cc=='@'){
                if (i==0) return false;
                cnt++;
                }
            else
                if (cc=='.' || cc>='A' && cc<='Z' || cc>='a' && cc<='z' || cc>='0' && cc<='9' || cc=='_')
                    continue;
                else
                    return false;
                }
        return cnt==1;
        }
    @Override
    public boolean parseAndSet(String ss) {
        boolean rez = valid(ss);
        if (rez) value = ss;
        return rez;
        }
    public static void main(String ss[]){
        System.out.println(new Mail("aaa@ddd.ccc").valid());
        System.out.println(new Mail("aaa@d_dd.ccc").valid());
        System.out.println(new Mail("@ddd.ccc").valid());
        System.out.println(new Mail("aaa@dd@d.ccc").valid());
    }
    public String toString(){
        return valid() ? value : "";
    }
}
