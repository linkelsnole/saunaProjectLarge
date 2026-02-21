package com.example.modbus_api.modbus.legacy;

import retrofit2.Response;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

public class Utils {
    public static int getRowsCount(String ss){
        int rowCount=1;
        int idx=0;
        while((idx = ss.indexOf("\n",idx+1))!=-1){
            rowCount++;
            }
        int rowCount2=1;
        idx=0;
        while((idx = ss.indexOf("$",idx+1))!=-1){
            rowCount2++;
            }
        return rowCount > rowCount2 ? rowCount : rowCount2;
        }
    public static boolean hasDigits(String ss){
        char cc[] = ss.toCharArray();
        for(char zz : cc)
            if (!(zz >= '0' && zz<='9'))
                return false;
         return true;
        }
    public static int nextMonth(int mnt){
        mnt++;
        if (mnt==13) mnt=1;
        return mnt;
        }
    public static int prevMonth(int mnt){
        mnt--;
        if (mnt==0) mnt=12;
        return mnt;
    }
    public static  int nextMonth(int mnt,int cnt){
        mnt+=cnt;
        if (mnt>12) mnt=mnt-12;
        return mnt;
    }
    public static String cutWithWords(String ss, int sz){
        if (ss.length()>sz)
            ss = ss.substring(0,sz);
        int idx = ss.lastIndexOf(" ");
        if (idx!=-1)
            ss = ss.substring(0,idx)+"...";
        return ss;
        }
     public static void toTextArea(String ss1, int sz, JTextArea mes){
        String ss = ss1;
        mes.setText("");
        while(true){
            if (ss.length()<sz){
                mes.append(ss);
                return;
                }
            String zz = ss.substring(0,sz);
            int idx = zz.lastIndexOf(" ");
            if (idx!=-1){
                mes.append(zz.substring(0,idx)+"\n");
                ss = ss.substring(idx+1);
                }
            else{
                mes.append(ss);
                return;
                }
            }
        }
    public static String generateToken(int sz){
        char cc[]=new char[sz];
        for(int i=0;i<cc.length;i++){
            int k=(int)(62*Math.random());
            if (k<26)
                cc[i]=(char)('A'+k);
            else
            if (k<52)
                cc[i]=(char)('a'+k-26);
            else
                cc[i]=(char)('0'+k-52);
        }
        return new String(cc);
        }
    public static String timeInMinToString(int time){
        if (time/(60*24)==0)
            return String.format("%2d:%2d",time/60,time%60);
        else
            return String.format("%d:%2d",time/60,time%60);
        }
    public static String timeInSecToString(int time){
        if (time/3600==0)
            return String.format("%2d:%2d",time/60,time%60);
        else
            return String.format("%2d:%2d:%2d",time/3600,(time/60)%60,time%60);
        }
    public static long getPID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null && processName.length() > 0) {
            try {
                return Long.parseLong(processName.split("@")[0]);
            }
            catch (Exception e) { return 0; }
        }
        return 0;
        }
    public static String nDigits(int vv, int ndig){
        String out = "";
        while(ndig--!=0){
            out = (char)(vv%10+'0')+out;
            vv/=10;
            }
        return out;
        }
    public static String httpError(Response res){
        String ss =   res.message()+" ("+res.code()+") ";
        try {
            ss+=res.errorBody().string();
            } catch (IOException e) {}
        return ss;
        }
    public static String createFatalMessage(Throwable ee) {
        return createFatalMessage(ee,10);
        }
    public static String createFatalMessage(Throwable ee, int stackSize) {
        String ss = ee.toString() + "\n";
        StackTraceElement dd[] = ee.getStackTrace();
        for (int i = 0; i < dd.length && i < stackSize; i++) {
            ss += dd[i].getClassName() + "." + dd[i].getMethodName() + ":" + dd[i].getLineNumber() + "\n";
        }
        String out = "Программная ошибка:\n" + ss;
        return out;
        }
    public static void printFatalMessage(Throwable ee) {
        System.out.println(createFatalMessage(ee));
        }
    public static String when(String ss){
        if (ss.length()==0) return ss;
        int idx=ss.length();
        char cc = ss.charAt(idx-1);
        switch (cc){
            case 'а': return ss.substring(0,idx-1)+'у';
            case 'о': return ss;
            case 'й': return ss.substring(0,idx-1)+'я';
            case 'я': return ss.substring(0,idx-1)+'ю';
            default:  return ss+'a';
            }
        }
    public static String toUpperFirst(String ss){
        return ss.substring(0,1).toUpperCase()+ss.substring(1);
        }
    public final static String FirstToUpper(String ss){
        return ss.substring(0,1).toUpperCase()+ss.substring(1);
        }

    public static String printConsoleOutput(boolean there,Process p, boolean isWin){
        Pair<ArrayList<String>,ArrayList<String>> res = printConsoleOutput(p,isWin);
        String out="";
        for(String ss : res.o1)
            out+=" error: "+ss+"\n";
        for(String ss : res.o2)
            out+= ss+"\n";
        if (there)
            System.out.print(out);
        return out;
    }
    public static Pair<ArrayList<String>,ArrayList<String>> printConsoleOutput(Process p, boolean isWin){
        ArrayList<String> out=new ArrayList<>(),out2=new ArrayList<>();
        Locale current = Locale.getDefault();
        InputStream is = p.getInputStream();
        InputStream ers = p.getErrorStream();
        BufferedReader stdInput=null;
        BufferedReader stdError=null;
        try {
            stdInput = new BufferedReader(new InputStreamReader(is, (isWin ? "Cp1251" : "UTF8")));
            stdError = new BufferedReader(new InputStreamReader(ers, (isWin ? "Cp1251" : "UTF8")));
            String temp1="";
            String temp2="";
            long clock = System.currentTimeMillis();
            while (p.isAlive() && System.currentTimeMillis()-clock < 10*1000){
                if((temp1 = stdInput.readLine())!=null){
                    out.add(temp1);
                    clock = System.currentTimeMillis();
                }
                if((temp2 = stdError.readLine())!=null){
                    out2.add(temp2);
                    clock = System.currentTimeMillis();
                }
            }
        } catch (IOException ee) {
            out2.add("printConsoleOutput: "+ee.toString());
            }
        return new Pair<>(out2,out);
        }
    //-----------------------------------------------------------------------------------------------------------------
    public static boolean executeCmdString(String title, String cmd){
        System.out.println(title+" execute: "+cmd);
        Pair<ArrayList<String>,ArrayList<String>> res = executeCmdString(cmd);
        for(String ss : res.o1)
            System.out.println(title+" error: "+ss);
        for(String ss : res.o2)
            System.out.println(title+": "+ss);
        return res.o1.size()==0;
        }
    public static Pair<ArrayList<String>,ArrayList<String>> executeCmdString(String cmd){
        StringTokenizer tok = new StringTokenizer(cmd," ");
        int cc = tok.countTokens();
        String vv[] = new String[cc];
        for(int i=0;i<cc;i++)
            vv[i]=tok.nextToken();
        final boolean isWin = System.getProperty("os.name").startsWith("Windows");
        Process p =null;
        try {
            p = new ProcessBuilder(vv).redirectErrorStream(true).start();
            //Runtime.getRuntime().exec(vv);
            Pair<ArrayList<String>,ArrayList<String>> out =  printConsoleOutput(p,isWin);
            p.destroy();
            return out;
        } catch (Exception e) {
            String zz = "ESS2Archive: Ошибка выполнения ком.строки "+e.toString();
            System.out.println(zz);
            if (p!=null)
                p.destroy();
            ArrayList<String> out = new ArrayList<>();
            out.add(zz);
            return new Pair<>(out,new ArrayList<>());
        }
    }




    public static void main(String a[]){
        System.out.println(when("Иванов"));
        System.out.println(when("Зуева"));
        System.out.println(when("Евгений"));
        System.out.println(when("Петр"));
        System.out.println(when("Мария"));
        System.out.println(when("Семенович"));
        System.out.println(getRowsCount("1\n2\n3"));
        }
}
