package com.example.modbus_api.modbus;

import com.example.modbus_api.modbus.model.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.*;

import java.util.ArrayList;
import com.example.modbus_api.dto.SystemEventDto;



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
                        throw UniException.io("Сеанс закрыт " + Utils.httpError(res), HTTPAuthorization);
                        }
                    String errorBody = res.errorBody() != null ? res.errorBody().string() : "";
                    throw UniException.io("Ошибка " + res.message()+" ("+res.code()+") "+ errorBody, res.code());
                    }
                return res.body();
                } catch (UniException ue) {
                    throw ue;
                } catch (Exception ex) {
                    throw new UniException(ex.getMessage(), 0);
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

        /** Прочитать список событий */
        @POST("/api/ess2/events/list")
        Call<ArrayList<SystemEventDto>> getEventsList(
            @Header("SessionToken") String token,
            @Query("lastminutes") int lastminutes,
            @Body com.example.modbus_api.modbus.legacy.IntegerList types
        );

        /** Прочитать URL обновления, сохранённый на контроллере */
        @GET("/api/ess2/upgrade/url/read")
        Call<JString> readUpgradeURL(@Header("SessionToken") String token);
        /** Записать URL обновления на контроллер */
        @POST("/api/ess2/upgrade/url/write")
        Call<JEmpty> writeUpgradeURL(@Header("SessionToken") String token, @Query("url") String url);
        /** Проверить/загрузить обновление */
        @POST("/api/ess2/upgrade/load")
        Call<ErrorList> upgradeLoad(@Header("SessionToken") String token, @Query("url") String url, @Query("force") boolean force);
        /** Проверить/установить обновление */
        @POST("/api/ess2/upgrade/execute")
        Call<ErrorList> upgradeExecute(@Header("SessionToken") String token, @Query("force") boolean force, @Query("self") boolean self);
        /** Состояние сервера (версия, релиз) */
        @GET("/api/serverstate")
        Call<ServerState> serverState(@Header("SessionToken") String token);

        }
}
