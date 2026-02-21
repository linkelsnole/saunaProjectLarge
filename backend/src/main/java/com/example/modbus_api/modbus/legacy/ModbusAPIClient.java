package com.example.modbus_api.modbus.legacy;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ModbusAPIClient {
    public final static int HTTPTimeOut=60;                     // Тайм-аут клиента
    public final static int HTTPOk = 200;
    public final static int HTTPAuthorization = 401;
    public final static int HTTPNotFound = 404;
    public final static int HTTPRequestError = 400;
    public final static int HTTPException = 500;
    public final static int HTTPServiceUnavailable = 503;  // Сервис недоступен
    //-------------------------------------------------------------------------------------------------------
    public abstract static class APICall<T> {
        public abstract Call<T> apiFun();
        public T call() throws UniException{
            try {
                Response<T> res = apiFun().execute();
                if (!res.isSuccessful()){
                    if (res.code()== HTTPAuthorization){
                        UniException.io("Сеанс закрыт " + Utils.httpError(res));
                        }
                    UniException.io("Ошибка " + res.message()+" ("+res.code()+") "+res.errorBody().string());
                    }
                return res.body();
                } catch (Exception ex) {
                    throw UniException.io(ex);
                    }
            }
        }
    //-------------------------------------------------------------------------------------------------------
    public interface RestAPIModbus {
        /** Активность клиента - в ответе - количество неподтвержденных уведомлений */
        @GET("/api/keepalive")
        Call<JInt> keepalive(@Header("SessionToken") String token);
        /** Авторизация по номеру телефона (логину) и паролю */
        @GET("/api/user/login/phone")
        Call<User> login(@Query("phone") String phone, @Query("pass") String pass);
        /** Прочитать регистр ПЛК по номеру */
        @GET("/api/ess2/register/value/read")
        Call<JInt> readESS2RegisterValue(@Header("SessionToken") String token,@Query("deviceName") String deviceName,@Query("unit") int unit,@Query("regnum") int regnum, @Query("equipControl") boolean equipControl);
        /** Записать регистр ПЛК по номеру */
        @POST("/api/ess2/register/value/write")
        Call<JEmpty> writeESS2RegisterValue(@Header("SessionToken") String token, @Query("deviceName") String deviceName, @Query("unit") int unit, @Query("regnum") int regnum, @Query("value") int value, @Query("equipControl") boolean equipControl);
        /** Прочитать регистры device по заданному списку */
        @POST("/api/ess2/registers/list/read")
        Call<IntegerList> readESS2RegistersValues(@Header("SessionToken") String token, @Query("deviceName") String deviceName,@Body UnitRegisterList list);
        /** Прочитать регистр ПЛК по номеру */
        @GET("/api/ess2/register/logunit/read")
        Call<JInt> readESS2RegisterByLogUnit(@Header("SessionToken") String token,@Query("logName") String deviceName,@Query("regnum") int regnum);
        /** Записать регистр ПЛК по номеру */
        @POST("/api/ess2/register/logunit/write")
        Call<JEmpty> writeESS2RegisterByLogUnit(@Header("SessionToken") String token,@Query("logName") String deviceName,@Query("regnum") int regnum,@Query("value") int value);
        /** Прочитать регистры device по заданному списку */
        @POST("/api/ess2/register/list/logunit/read")
        Call<IntegerList> readESS2RegisterListByLogUnit(@Header("SessionToken") String token, @Query("logName") String deviceName,@Body UnitRegisterList list);
        /** получить объекты - имя класса-XML-запрос */
        @GET("/api/entity/list/query")
        Call<ArrayList<DBRequest>> getEntityListByQuery(@Header("SessionToken") String token, @Query("classname") String classname, @Query("xmlquery") String xmlQuery, @Query("level") int level);
        // ------------------- 105.01 Список событий ---------------------------------------------------------------------------------------
        @POST("/api/ess2/events/list")
        Call<ArrayList<ArchESSEvent>> getEventsList(@Header("SessionToken") String token, @Query("lastminutes") int lastminutes, @Body IntegerList types);
        @GET("/api/ess2/module/health")
        Call<PLCHealtsData> getModuleHealth(@Header("SessionToken") String token);
        }
    public static Object startClient(String ip, String port, Class restAPI) throws UniException {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(HTTPTimeOut, TimeUnit.SECONDS)
                .connectTimeout(HTTPTimeOut, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://"+ip+":"+port)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return  retrofit.create(restAPI);
        }
    public static ArrayList<ArchESSEvent> repaintValues(RestAPIModbus api3,String token,IntegerList types) throws UniException {
        ArrayList<ArchESSEvent> oo = new APICall<ArrayList<ArchESSEvent>>(){
            @Override
            public Call<ArrayList<ArchESSEvent>> apiFun() {
                return api3.getEventsList(token,10,types);
            }
        }.call();
        System.out.println("Прочитано событий "+oo.size());
        for(ArchESSEvent event : oo)
            System.out.println(event);
        return oo;
        }
    public static void main(String ss[]) throws UniException {
        final String ip = "172.16.7.109";
        //final String ip = "212.164.86.237";
        //final String ip = "localhost";
        final String port = "4567";
        //final String port = "4569";
        final RestAPIModbus api3 = (RestAPIModbus) startClient(ip, port, RestAPIModbus.class);    // Короткое API для регистров Modbus
        final String sensorDevice = "Sensors";
        final String controlDevice = "AirPLC";
        final String logUnit2 = "AirPLC";
        final String logUnit = "T1";
        JInt vv = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.keepalive("");
            }
        }.call();
        System.out.println("before keepAlive="+vv.getValue());
        //----------------------------- Опрос здоровья -----------------------------------------------------------------
        User user = new APICall<User>() {
            @Override
            public Call<User> apiFun() {
                return api3.login("9131111111", "1234");
            }
        }.call();
        String token = user.getSessionToken();
        System.out.println("token=" + token);
        vv = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.keepalive(token);
            }
        }.call();
        System.out.println("after keepAlive="+vv.getValue());
        PLCHealtsData oo = new APICall<PLCHealtsData>() {
            @Override
            public Call<PLCHealtsData> apiFun() {
                return api3.getModuleHealth(token);
            }
        }.call();
        System.out.println("Здоровье оборудования\n" + oo.toString());
        //----------------------------- Чтение событий -------------------------------------------------------------------
        repaintValues(api3, token, new IntegerList());
        IntegerList types = new IntegerList();
        types.add(16);      // МУ
        types.add(3);      // МУ
        types.add(4);      // МУ
        repaintValues(api3, token, types);
        //----------------- Работа с логическими устройствами -----------------------------------------------------
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, logUnit, 0x0001);
            }
        }.call();
        System.out.println("T1: температура 0x0001 = " + val.getValue() / 10.0);
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, logUnit2, 0x0100);
            }
        }.call();
        System.out.println("AirPLC T1: температура 0x0100 = " + val.getValue() / 10.0);
        /*---------------- Сюда записывать нельзя
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x0005,66);
                }
            }.call();
         */
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, logUnit, 0x0005);
            }
        }.call();
        System.out.println("T1: уставка 0x0005 = " + val.getValue());
        //------------------------------ Чтение расписания
        final UnitRegisterList list = new UnitRegisterList();
        list.setUnitIdx(0);
        for (int reg = 0x0400; reg <= 0x0490; reg += 0x10) {
            for (int i = 0; i <= 10; i++)
                list.add(reg + i);
        }
        long tt = System.currentTimeMillis();
        IntegerList val2 = new APICall<IntegerList>() {
            @Override
            public Call<IntegerList> apiFun() {
                return api3.readESS2RegisterListByLogUnit(token, logUnit2, list);
            }
        }.call();
        for (int i = 0; i < val2.size(); i += 2) {
            System.out.print(String.format("%x=%x ", val2.get(i), val2.get(i + 1)));
            if ((i + 2) % 20 == 0)
                System.out.println();
        }
        System.out.println("МУ: Расписание: time(ms)=" + (System.currentTimeMillis() - tt));
        //----------------------------- Чтение регистров МУ
        final UnitRegisterList list3 = new UnitRegisterList();
        list3.setUnitIdx(0);
        list3.add(0x004C);       // C_Energy0
        list3.add(0x004D);       // C_Energy1
        list3.add(0x0047);       // VM1
        list3.add(0x0048);       // VM2
        list3.add(0x0062);       // KPDRecupOpt
        list3.add(0x0010);       // State
        list3.add(0x0100);       // T1_Temp
        list3.add(0x0110);       // T2_Temp
        list3.add(0x0120);       // T3_Temp
        list3.add(0x0130);       // T4_Temp
        list3.add(0x0140);       // T5_Temp
        list3.add(0x0194);       // EM1_Pa
        list3.add(0x0195);       // EM1_Ua
        list3.add(0x0196);       // EM1_Ia
        list3.add(0x0197);       // EM1_Fr
        int level = 1000;
        System.out.println("Экстраординарные задержки >" + level + " мс");
        long t3 = System.currentTimeMillis();
        for (int k = 0; k < 10; k++) {
            tt = System.currentTimeMillis();
            val2 = new APICall<IntegerList>() {
                @Override
                public Call<IntegerList> apiFun() {
                    return api3.readESS2RegisterListByLogUnit(token, logUnit2, list3);
                }
            }.call();
            /*
            for(int i=0;i<val2.size();i+=2){
                System.out.print(String.format("%x=%d(0x%x) ",val2.get(i),val2.get(i+1),val2.get(i+1)));
                if ((i+2)%20==0)
                    System.out.println();
                }
            System.out.println();
             */
            //---------------------------- Экстраординарные задержки
            long t2 = (System.currentTimeMillis() - tt);
            if (t2 > level) {
                System.out.println("МУ: Данные экрана[" + k + "]: период:" + (System.currentTimeMillis() - t3) + " мс задержка=" + t2 + "мс");
                t3 = System.currentTimeMillis();
            }
        }
    }
}



