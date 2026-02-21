package com.example.modbus_api.modbus.legacy;

import java.io.IOException;
import java.sql.SQLException;

public class UniException extends Exception{
    private final static String exceptLevel[]={
            "Пользователь",
            "Ошибка исполнения",
            "Программная ошибка",
            "Фатальная ошибка"
    };
    public final static int warning=0;
    public final static int runTime=1;
    public final static int bug=2;
    public final static int fatal=3;
    //------------------ Типовые ошибки ----------------------------------------
    public final static String sysCode="Ошибка кода исполнительной системы";
    public final static String sql="Ошибка базы данных";
    public final static String net="Ошибка сети";
    public final static String api="Ошибка запроса сервера данных";
    public final static String format="Ошибка формата даннных";
    public final static String noFunc="Функция не реализована";
    public final static String other="Прочие ошибки";
    public final static String indirect="Ошибка удаленной компоненты";
    public final static String settings="Ошибка настроек";
    public final static String io="Ошибка в/в";
    public final static String userData="Ошибка пользователя";
    public final static String userCode="Ошибка кода пользователя";
    public final static String userVars="Ошибка данных пользователя";
    //--------------------------------------------------------------------------
    private String sysMessage="";
    private String userMessage="";
    private int type=0;
    public int errorCode=0;
    //--------------------------------------------------------------------------
    public int getErrorCode() { return errorCode;}
    public String getSysMessage() {
        return sysMessage;
    }
    public void addSysMessage(String ss) {
        sysMessage+=" "+ss;
    }
    public String getUserMessage() {
        return userMessage;
    }
    public int getType() {
        return type;
    }
    public String toString(){
        return exceptLevel[type]+":"+(errorCode!=0 ? errorCode : "")+" "+userMessage+"\n"+sysMessage;
        }
    //--------------------------------------------------------------------------
    public UniException(int type, String message, String sysMessage){
        this.type=type;
        this.userMessage=message;
        this.sysMessage=sysMessage;
        }
    public UniException(int type, String message, String sysMessage, int code){
        this.type=type;
        this.userMessage=message;
        this.sysMessage=sysMessage;
        errorCode = code;
        }
    public UniException(int type, String message, Throwable ee, boolean stackTrace){
        this.type=type;
        this.userMessage=message;
        sysMessage="";
        if (ee==null)
            return;
        sysMessage=ee.getMessage()+"\n"+ee.toString();
        if (!stackTrace)
            return;
        StackTraceElement dd[]=ee.getStackTrace();
        for (int i = 0; dd != null && i < dd.length; i++) {
            String cls = dd[i].getClassName();
            sysMessage += "\n"+cls + "." + dd[i].getMethodName() + ":" + dd[i].getLineNumber();
        }
    }
    public UniException(int type, String message){
        this(type,message,"");
    }
    public UniException(int type){
        this(type,"","");
    }
    public static UniException fatal(String mes, Throwable ee){
        return new UniException(bug, mes, ee, true);
    }
    public static UniException fatal(Throwable ee){
        return new UniException(bug, sysCode, ee, true);
    }
    public static UniException fatal(String mes){
        return new UniException(bug, sysCode+ ":" + mes, null, false);
    }
    public static UniException warning(String mes){
        return new UniException(warning,mes);
    }
    public static UniException sql(Throwable ee){ return new UniException(bug,sql,ee,false); }
    public static UniException sql(String mes){
        return new UniException(bug,sql,mes);
    }
    public static UniException net(Throwable ee){
        return new UniException(runTime,net,ee,false);
    }
    public static UniException net(String mes){
        return new UniException(runTime,net,mes);
    }
    public static UniException api(Throwable ee){
        return new UniException(runTime,api,ee,false);
    }
    public static UniException api(String mes){
        return new UniException(runTime,api,mes);
    }
    public static UniException format(String mes){
        return new UniException(bug,format,mes);
    }
    public static UniException format(String mes, int code){
        return new UniException(bug,format,mes,code);
    }
    public static UniException userFormat(String mes){
        return new UniException(bug,format,mes);
    }
    public static UniException user(Throwable ee){
        return new UniException(warning,userData,ee,true);
    }
    public static UniException noFunc(){
        return new UniException(bug,noFunc);
    }
    public static UniException noFunc(String mes){
        return new UniException(bug,noFunc,mes);
    }
    public static UniException bug(String mes){
        return new UniException(bug,sysCode,mes);
    }
    public static UniException bug(Throwable ee){
        return new UniException(bug,sysCode,ee,false);
    }
    public static UniException user(String mes){
        return new UniException(warning,userData,mes);
    }
    public static UniException code(String mes){
        return new UniException(runTime,userCode,mes);
    }
    public static UniException vars(String mes){
        return new UniException(runTime,userVars,mes);
    }
    public static UniException other(){
        return new UniException(bug,other);
    }
    public static UniException other(Throwable ee){
        return new UniException(bug, other, ee, true);
    }
    public static UniException other(String mes){
        return new UniException(bug,mes);
    }
    public static UniException indirect(String mes){
        return new UniException(runTime,indirect,mes);
    }
    public static UniException config(String mes){
        return new UniException(warning,settings,mes);
    }
    public static UniException io(Throwable ee){ return new UniException(runTime,io,ee,false);}
    public static UniException io(String mes){ return new UniException(runTime,io,mes);}
    public static UniException total(Throwable ee){
        if (ee instanceof UniException){
            UniException vv=(UniException)ee;
            vv.sysMessage = vv.userMessage + ":" +vv.sysMessage;
            vv.userMessage = indirect;
            return vv;
        }
        if (ee instanceof SQLException)
            return sql(ee);
        if (ee instanceof IOException)
            return io(ee);
        if (ee instanceof Error)
            return fatal(ee);
        return other(ee);
    }
}
