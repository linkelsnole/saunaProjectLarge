package com.example.modbus_api.service;

import com.example.modbus_api.config.ModbusProperties;
import com.example.modbus_api.modbus.ModbusAPIClient;
import com.example.modbus_api.modbus.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import okhttp3.OkHttpClient;
import okhttp3.ConnectionPool;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.scheduling.annotation.Scheduled;
import com.example.modbus_api.modbus.model.JInt;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ModbusClientAdapter {
    
    private final ModbusAPIClient.RestAPIModbus api;
    private final OkHttpClient httpClient;
    private final ModbusProperties config;
    private volatile String sessionToken;
    private final Object lock = new Object();

    
    @Autowired
    public ModbusClientAdapter(ModbusProperties config) {
        this.config = config;
        this.httpClient = createOptimizedHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://" + config.getIp() + ":" + config.getPort())
                .addConverterFactory(GsonConverterFactory.create())
                .client(this.httpClient)
                .build();
        this.api = retrofit.create(ModbusAPIClient.RestAPIModbus.class);
    }
    
    private OkHttpClient createOptimizedHttpClient() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
											.connectTimeout(60, TimeUnit.SECONDS)
											.readTimeout(60, TimeUnit.SECONDS)
											.writeTimeout(60, TimeUnit.SECONDS)
											.connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
											.retryOnConnectionFailure(false);

        if (config.isEnableHttpLogging()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(logging);
        }
        
				System.out.println("Modbus API: Настройка HTTP клиента - ConnectionPool(ОТКЛЮЧЕН), " +
				"таймауты: connect=60s, read=60s, write=60s");

        return clientBuilder.build();
    }
    

    private void initializeSession() throws IOException {
        try {
            System.out.println("Modbus API: Сессия не активна. Попытка авторизации...");
            long authStart = System.currentTimeMillis();
            
            User user = new ModbusAPIClient.APICall<User>() {
                @Override
                public retrofit2.Call<User> apiFun() {
                    return api.login(config.getUsername(), config.getPassword());
                }
            }.call();
            
            long authEnd = System.currentTimeMillis();
            
            if (user == null || user.getSessionToken() == null) {
                throw new IOException("Авторизация не удалась: API вернул пустой ответ.");
            }

            this.sessionToken = user.getSessionToken();
            System.out.println("Modbus API: Авторизация выполнена за " + (authEnd - authStart) + "ms, токен получен.");
        
        } catch (Exception e) {
            System.err.println("Modbus API: Ошибка авторизации - " + e.getMessage());
            
            String errorType = "unknown";
            if (e.getMessage().contains("Connect timed out")) {
                errorType = "connection_timeout";
                System.err.println("Modbus API: Устройство не отвечает на подключение в течение " + config.getConnectTimeout() + " секунд");
            } else if (e.getMessage().contains("Connection refused")) {
                errorType = "connection_refused";
                System.err.println("Modbus API: Устройство отклонило подключение");
            } else if (e.getMessage().contains("No route to host")) {
                errorType = "network_unreachable";
                System.err.println("Modbus API: Сетевая недоступность устройства");
            }
            
            if (!errorType.equals("unknown")) {
                System.err.println("Modbus API: Тип ошибки: " + errorType);
            }
            
            throw new IOException("Ошибка авторизации в Modbus API (" + errorType + "): " + e.getMessage(), e);
        }
    }
    
    /**
     * Обеспечивает наличие активной сессии
     */
    public void ensureConnection() throws IOException {
        if (sessionToken == null) {

            synchronized (lock) {
                if (sessionToken == null) {
                    initializeSession();
                }
            }
        }
    }
    
    /**
     * Переподключение - сброс токена и повторная авторизация
     */
    private void reconnect() throws IOException {
        synchronized (lock) {
            System.out.println("Modbus API: Выполняется переподключение. Сброс токена и повторная авторизация...");
            sessionToken = null;
            long reconnectStart = System.currentTimeMillis();
            initializeSession();
            long reconnectEnd = System.currentTimeMillis();
            System.out.println("Modbus API: Переподключение успешно завершено за " + (reconnectEnd - reconnectStart) + "ms");
        }
    }

    /**
     * Проверяет, связана ли ошибка с авторизацией или обрывом соединения
     */
    private boolean isConnectionOrAuthError(Exception e) {
        if (e == null) {
            return false;
        }

        if (e instanceof UniException) {
            int code = ((UniException) e).getHttpCode();
            if (code == 401 || code == 403) {
                return true;
            }
        }

        if (e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.ConnectException) {
            return true;
        }

        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        if (message.contains("Failed to connect")) {
            return true;
        }

        return false;
    }
    
    /**
     * Выполняет чтение регистра без retry логики
     */
    private int performRead(String logUnit, int register) throws IOException {
        JInt result = new ModbusAPIClient.APICall<JInt>() {
            @Override
            public retrofit2.Call<JInt> apiFun() {
                return api.readESS2RegisterByLogUnit(sessionToken, logUnit, register);
            }
        }.call();

        if (result == null) {
            throw new IOException("Ошибка чтения регистра " + Integer.toHexString(register) + ": API вернул пустой ответ.");
        }
        
        return result.getValue();
    }
    
    /**
     * Выполняет запись регистра без retry логики
     */
    private void performWrite(String logUnit, int register, int value) throws IOException {
        new ModbusAPIClient.APICall<JEmpty>() {
            @Override
            public retrofit2.Call<JEmpty> apiFun() {
                return api.writeESS2RegisterByLogUnit(sessionToken, logUnit, register, value);
            }
        }.call();
    }
    
    /**
     * Выполняет пакетное чтение регистров без retry логики
     */
    private Map<Integer, Integer> performReadRegisters(String logUnit, List<Integer> registers) throws IOException {
        UnitRegisterList unitRegisterList = new UnitRegisterList();
        unitRegisterList.setUnitIdx(0);

        for (Integer reg : registers) {
            unitRegisterList.add(reg);
        }

        System.out.println("--> Вызов эндпоинта '.../register/list/logunit/read' для '" + logUnit + "' с " + registers.size() + " регистрами.");

        IntegerList result = new ModbusAPIClient.APICall<IntegerList>() {
            @Override
            public retrofit2.Call<IntegerList> apiFun() {
                return api.readESS2RegisterListByLogUnit(sessionToken, logUnit, unitRegisterList);
            }
        }.call();

        int responseSize = (result != null) ? result.size() : 0;
        System.out.println("<-- Получен ответ от '" + logUnit + "'. Элементов в ответе: " + responseSize);

        return parseRegisterListResponse(result);
    }
    
    public int readRegister(String logUnit, int register) throws IOException {

				try {
        ensureConnection();
        try {
            return performRead(logUnit, register);
        } catch (Exception e) {
            if (isConnectionOrAuthError(e)) {
                System.out.println("Modbus API: Ошибка соединения или аутентификации при чтении (" + 
                                 e.getClass().getSimpleName() + ": " + e.getMessage() + "). Повторная авторизация...");
                try {
                    reconnect();
                    return performRead(logUnit, register); 
                } catch (IOException reconnectException) {
                    throw new IOException("Не удалось выполнить переподключение после сбоя при чтении регистра " + 
                                        Integer.toHexString(register) + ": " + reconnectException.getMessage(), reconnectException);
                }
            }
            throw new IOException("Ошибка чтения регистра " + Integer.toHexString(register) + ": " + e.getMessage(), e);
        }
				} finally {

				}
    }
    
    public void writeRegister(String logUnit, int register, int value) throws IOException {

				try {
        ensureConnection();
        try {
            performWrite(logUnit, register, value);
        } catch (Exception e) {
            if (isConnectionOrAuthError(e)) {
                System.out.println("Modbus API: Ошибка соединения или аутентификации при записи (" + 
                                 e.getClass().getSimpleName() + ": " + e.getMessage() + "). Повторная авторизация...");
                try {
                    reconnect();
                    performWrite(logUnit, register, value);
                } catch (IOException reconnectException) {
                    throw new IOException("Не удалось выполнить переподключение после сбоя при записи регистра " + 
                                        Integer.toHexString(register) + ": " + reconnectException.getMessage(), reconnectException);
                }
            } else {
                throw new IOException("Ошибка записи регистра " + Integer.toHexString(register) + ": " + e.getMessage(), e);
            }
        }
				} finally {

				}
    }

    
    /**
     * Проверка доступности соединения
     */
    public boolean isConnected() {
        try {
            if (sessionToken == null) {
                return false;
            }
            
            new ModbusAPIClient.APICall<JInt>() {
                @Override
                public retrofit2.Call<JInt> apiFun() {
                    return api.keepalive(sessionToken);
                }
            }.call();
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public Map<Integer, Integer> readRegisters(String logUnit, List<Integer> registers) throws IOException {

			try {
			ensureConnection();
        try {
            return performReadRegisters(logUnit, registers);
        } catch (Exception e) {

            if (isConnectionOrAuthError(e)) {
                System.out.println("Modbus API: Ошибка соединения или аутентификации при пакетном чтении (" + 
                                 e.getClass().getSimpleName() + ": " + e.getMessage() + "). Повторная авторизация...");
                try {
                    reconnect();
                    return performReadRegisters(logUnit, registers); 
                } catch (IOException reconnectException) {
                    throw new IOException("Не удалось выполнить переподключение после сбоя при пакетном чтении из " + 
                                        logUnit + ": " + reconnectException.getMessage(), reconnectException);
                }
            }
            throw new IOException("Ошибка пакетного чтения регистров из " + logUnit + ": " + e.getMessage(), e);
			}
			} finally {

			}
    }


		private Map<Integer, Integer> parseRegisterListResponse(IntegerList list) throws IOException {
			if (list == null || list.isEmpty()) {
				throw new IOException("Modbus API вернул пустой ответ. Вероятно, сессия истекла.");
			}

			if (list.size() % 2 != 0) {
					throw new IOException("Некорректный ответ от API: нечетное количество элементов.");
			}
			
			Map<Integer, Integer> resultMap = new HashMap<>();
			for (int i = 0; i < list.size(); i += 2) {
					resultMap.put(list.get(i), list.get(i + 1));
			}
			return resultMap;
		}
		
		public ModbusAPIClient.RestAPIModbus getApi() {
			return api;
		}

		public String getSessionToken() {
			return sessionToken;
		}

		@Scheduled(fixedRate = 600000)
    public void performKeepAlive() {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return; 
        }
        System.out.println("[KEEP-ALIVE] Выполняется запрос для поддержания сессии...");
        try {
            JInt state = new ModbusAPIClient.APICall<JInt>() {
                @Override
                public retrofit2.Call<JInt> apiFun() {

                    return api.keepalive(sessionToken);
                }
            }.call();
            if (state != null) {
                System.out.println("[KEEP-ALIVE] Сессия успешно поддержана. Ответ сервера: " + state.getValue());
            } else {
                 System.out.println("[KEEP-ALIVE] ВНИМАНИЕ: Запрос выполнен, но сервер вернул null.");
            }

        } catch (Exception e) {
            System.err.println("[KEEP-ALIVE] Ошибка при выполнении запроса: " + e.getMessage() + ". Возможно, сессия уже истекла.");
        }
    }

		
		@Scheduled(initialDelay = 3600000, fixedRate = 3600000) // 5 hours = 18000000ms
		public void forcedRecconect() {
			System.out.println("[SCHEDULED RECONNECT] Начинается плановая обнова токена сессии...");
			try {
				reconnect();
				System.out.println("[SCHEDULED RECONNECT] Токен сессии был успешно обновлен");
			} catch (Exception e) {
				System.err.println("[SCHEDULED RECONNECT] Ошибка при выполнении запроса: " + e.getMessage() + ". Возможно, сессия уже истекла");
			}
		}
	public void reconnectButton() throws IOException {
		try {
			reconnect();
			System.out.println("[RECONNECT BUTTON] Реконнект выполнен успешно");
		} catch (IOException e) {
			System.err.println("[RECONNECT BUTTON] Ошибка при выполнении запроса: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			System.err.println("[RECONNECT BUTTON] Ошибка при выполнении запроса: " + e.getMessage());
			throw new IOException("Ошибка при ручном переподключении: " + e.getMessage(), e);
		}
	}

    public void sabotageSession() {
           System.out.println("{TEST} ПЛОХОЙ ТОКЕН");
           this.sessionToken = "NAHUI_BLYAT";
    }
} 