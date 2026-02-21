package com.example.modbus_api.modbus.legacy;

import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;


import java.text.ParseException;
import java.text.SimpleDateFormat;

public class OwnDateTime {
    private long timeInMS=0;
    transient DateTime dd;
    transient private boolean refresh=false;
    //---------------------------------------------------------------------------
    public String toStringValue() {
        return ""+timeInMS;
        }
    public void parseValue(String ss) throws Exception {
        timeInMS = Long.parseLong(ss);
        refresh=false;
        refresh();
        }
    //---------------------------------------------------------------------------
    private void refresh(){
        if (refresh || !dateTimeValid()) return;
        dd = new DateTime(timeInMS);
        refresh=true;
        }
    public boolean equals(OwnDateTime two){
        if (!dateTimeValid() || !two.dateTimeValid())
            return false;
        return timeInMS == two.timeInMS;
        }
    public OwnDateTime(String date){
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        try {
            timeInMS = format.parse(date).getTime();
            dd = new DateTime(timeInMS);
            refresh=true;
            } catch (ParseException e) {}
        }
    public OwnDateTime clone(){ return  new OwnDateTime(timeInMS); }
    public void onlyDate(){
        dd = new DateTime(dd.getYear(),dd.getMonthOfYear(),dd.getDayOfMonth(),0,0);
        timeInMS = dd.getMillis();
        }
    public boolean dateTimeValid(){ return timeInMS!=0; }
    public OwnDateTime(){
        dd = new DateTime();
        timeInMS = dd.getMillis();
        }
    public OwnDateTime(boolean ff){
        dd = new DateTime(0);
        timeInMS = 0;
        }
    public OwnDateTime(int hh, int mm){
        refresh=true;
        dd = new DateTime();
        dd = new DateTime(dd.getYear(),dd.getMonthOfYear(),dd.getDayOfMonth(),hh,mm);
        timeInMS = dd.getMillis();
        }
    public OwnDateTime(int day, int mm, int yy, int hh, int min){
        refresh=true;
        dd = new DateTime(yy,mm,day,hh,min);
        timeInMS = dd.getMillis();
        }
    public OwnDateTime(int day, int mm, int yy, int hh, int min, int sec){
        refresh=true;
        dd = new DateTime(yy,mm,day,hh,min,sec);
        timeInMS = dd.getMillis();
        }
    public OwnDateTime(int day, int mm, int yy){
        refresh=true;
        dd = new DateTime(yy,mm,day,0,0);
        timeInMS = dd.getMillis();
        }
    public void setDate(int day, int mm, int yy){
        dd = new DateTime(yy,mm,day,0,0);
        timeInMS = dd.getMillis();
        refresh=true;
        }
    public void changeDateSaveTime(OwnDateTime two){
        dd = new DateTime(two.year(),two.month(),two.day(),dd.getHourOfDay(),dd.getMinuteOfHour());
        timeInMS = dd.getMillis();
        refresh=true;
    }
    public OwnDateTime(long ms){
        timeInMS = ms; dd = new DateTime(timeInMS);
        refresh=true;
        }
    public int compareDate(OwnDateTime two){
        refresh();
        two.refresh();
        if (year()!=two.year())
            return year()-two.year();
        if (month()!=two.month())
            return month()-two.month();
        return day()-two.day();
        }
    public int elapsedTimeInSec(){
        refresh();
        long tt1=new OwnDateTime().timeInMS();
        int tt2 =  (int)((tt1-timeInMS())/1000);
        return tt2 < 0 ? 0 : tt2;
        }
    public DateTime date(){ refresh(); return dd; }
    public long getTimeInMS(){ return timeInMS; }
    public long timeInMS(){ return timeInMS; }
    public int monthDifference(OwnDateTime two){
        if (!dateTimeValid() || !two.dateTimeValid())
            return 0;
        return year()*12+month()-two.year()*12-two.month();
        }
    public String monthToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("MM-yyyy")); }
    public String timeToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("HH:mm")); }
    public String timeFullToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("HH:mm:ss")); }
    public String dateToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("dd-MM-yyyy")); }
    public String toString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("yyyy-MM-dd_HH.mm")); }
    public String toString2(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("yyyy-MM-dd_HH.mm.ss")); }
    public String dateTimeToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm")); }
    public String fullToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss")); }
    public String fullTimeToString(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("HH:mm:ss"))+":"+timeInMS%1000; }
    public String dateTimeToString2(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("dd-MM HH:mm")); }
    public String toStringSec(String spDate,String spTime){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("yyyy"+spDate+"MM"+spDate+"dd_HH"+spTime+"mm"+spTime+"ss")); }
    public String toStringMark(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")); }
    public String toStringMarkPoints(){
        refresh();
        return !dateTimeValid() ? "---" : dd.toString(DateTimeFormat.forPattern("yyyy.MM.dd.HH.mm.ss")); }
    public String toStringSec(){
        return toStringSec("-","."); }
    public int year(){ refresh(); return !dateTimeValid() ? 0 : dd.getYear(); }
    public int month(){ refresh(); return !dateTimeValid() ? 0 : dd.getMonthOfYear(); }
    public int day(){ refresh(); return !dateTimeValid() ? 0 : dd.getDayOfMonth(); }
    public int second(){ refresh(); return !dateTimeValid() ? 0 : dd.getSecondOfMinute(); }
    public int dayOfWeek(){ refresh(); return dd.getDayOfWeek(); }
    public void incMonth(){ refresh(); dd = dd.plusMonths(1); timeInMS = dd.getMillis(); }
    public void decMonth(){ refresh(); dd = dd.minusMonths(1); timeInMS = dd.getMillis(); }
    public OwnDateTime day(int vv){
        refresh(); dd=dd.withDayOfMonth(vv);
        timeInMS = dd.getMillis(); return this;}
    public OwnDateTime hour(int vv){
        refresh(); dd=dd.withHourOfDay(vv);
        timeInMS = dd.getMillis(); return this; }
    public OwnDateTime minute(int vv){
        refresh(); dd=dd.withMinuteOfHour(vv); timeInMS = dd.getMillis();
        return this; }
    public OwnDateTime month(int vv){
        refresh(); dd=dd.withMonthOfYear(vv); timeInMS = dd.getMillis();
        return this; }
    public OwnDateTime year(int vv){
        refresh(); dd=dd.withYear(vv); timeInMS = dd.getMillis();
        return this; }
    public int hour(){ refresh(); return !dateTimeValid() ? 0 : dd.getHourOfDay(); }
    public int minute(){ refresh(); return !dateTimeValid() ? 0 : dd.getMinuteOfHour(); }
    public void plusDays(int vv){ refresh(); dd=dd.plusDays(vv); timeInMS = dd.getMillis(); }
    public void minusDays(int vv){ refresh(); dd=dd.minusDays(vv); timeInMS = dd.getMillis(); }
    public void setOnlyDate(){
        refresh();
        int d1 = dd.getDayOfMonth();
        int m1 = dd.getMonthOfYear();
        int yy = dd.getYear();
        dd = new DateTime(yy,m1,d1,0,0);
        timeInMS = dd.getMillis();
        }
    public int getOnlyTime(){
        return hour()*60+minute();
        }
    public void setOnlyTime(int time){
        minute(time%60);
        hour(time/60);
        }
    public boolean isToday(){
        OwnDateTime tt = new OwnDateTime();
        tt.onlyDate();
        OwnDateTime tt2 = new OwnDateTime(timeInMS);
        tt2.onlyDate();
        return tt.timeInMS==tt2.timeInMS;
        }
    public long timeOfDayInMin(){
        return hour()*60+minute();
        }
    //------------------------------------------------------------------------------------------------------------------
    public void afterLoad() {
        dd = new DateTime(timeInMS);
        refresh=true;
        }
    /*
    @Override
    public void putData(String prefix, org.bson.Document document, int level, I_MongoDB mongo) throws UniException {
        putDBValues(prefix,document,level,mongo);
        }
    @Override
    public void getData(String prefix, org.bson.Document res, int level, I_MongoDB mongo) throws UniException {
        getDBValues(prefix,res);
        afterLoad();
        }
    //----------------- Импорт/экспорт Excel ------------------------------------------------------------
    @Override
    public void getData(Row row, ExCellCounter cnt) throws UniException{
        //getXMLValues(row, cnt);
        //String ss = row.getCell(cnt.getIdx()).getStringCellValue();
        //if (ss.equals("0"))                 // Проверить импорт
        //    dd = new DateTime(false);
        //else{
        //    dd = new DateTime(ss);
        //    timeInMS = dd.getMillis();
        //    }
        //refresh=true;
        timeInMS = (long) row.getCell(cnt.getIdx()).getNumericCellValue();
        refresh = false;
        }
    @Override
    public void putData(Row row, ExCellCounter cnt) throws UniException{
        //putXMLValues(row, cnt);
        row.createCell(cnt.getIdx()).setCellValue(
            //timeInMS==0 ? "0" : dd.toString(DateTimeFormat.forPattern("dd.MM.YYYY HH:mm"))
            timeInMS
            );
        }
    @Override
    public void putHeader(String prefix, ArrayList<String> list) throws UniException{
        putXMLHeader(prefix,list);
        }
    */
    //-------------------------------------------------------------------------------------------------------------------
    /*  ПОКА ОСТАВИМ В ВИДЕ long
    @Override
    public void putData(Row row, ExCellCounter cnt) throws UniException {
        row.createCell(cnt.getIdx()).setCellValue(
                timeInMS==0 ? "0" : dd.toString(DateTimeFormat.forPattern("dd.MM.YYYY HH:mm")));
        }
    @Override
    public void getData(Row row, ExCellCounter cnt) throws UniException {
        String ss = row.getCell(cnt.getIdx()).getStringCellValue();
        if (ss.equals("0"))                 // Проверить импорт
            dd = new DateTime(false);
        else{
            dd = new DateTime(ss);
            timeInMS = dd.getMillis();
            }
        refresh=true;
        }
    */
    public static String time(int duration){
        return String.format("%2d:%2d",duration/60,duration%60);
        }
    public static void main(String a[]){
        OwnDateTime xx = new OwnDateTime();
        System.out.println(xx.date().toString(DateTimeFormat.forPattern("yyyy-MM-dd_HH:mm:ss")));
        System.out.println(new Gson().toJson(xx));
        OwnDateTime tt = new OwnDateTime();
        System.out.println(tt.isToday());
        tt.hour(12);
        System.out.println(tt.isToday());
        tt.incMonth();
        System.out.println(tt.isToday());
        System.out.println(new OwnDateTime().timeOfDayInMin());
        tt.setDate(1,11,2019);
        tt.incMonth();
        System.out.println(tt);
    }

}
