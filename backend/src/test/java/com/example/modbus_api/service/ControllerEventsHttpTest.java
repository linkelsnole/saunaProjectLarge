package com.example.modbus_api.service;

import com.example.modbus_api.config.ModbusProperties;
import com.example.modbus_api.dto.SystemEventDto;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Real Retrofit/Gson exchange against a loopback-only fake data server, never the installation. */
class ControllerEventsHttpTest {
 HttpServer server;ModbusClientAdapter client;
 final List<String> paths=Collections.synchronizedList(new ArrayList<>());
 final List<String> eventBodies=Collections.synchronizedList(new ArrayList<>());
 volatile String response="[]";volatile int status=200;volatile boolean expireFirst=false;
 AtomicInteger events=new AtomicInteger();AtomicInteger logins=new AtomicInteger();
 @BeforeEach void start() throws Exception {
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{
   String path=exchange.getRequestURI().getPath();paths.add(exchange.getRequestURI().toString());String body="{}";int code=200;
   if(path.equals("/api/user/login/phone")){logins.incrementAndGet();body="{\"sessionToken\":\"synthetic-session\"}";}
   else if(path.equals("/api/ess2/events/list")){
    eventBodies.add(read(exchange.getRequestBody()));int count=events.incrementAndGet();code=expireFirst&&count==1?401:status;body=response;
   } else if(path.equals("/api/ess2/register/logunit/read")){body="{\"value\":42}";}
   else if(path.equals("/api/ess2/register/list/logunit/read")){body="[769,0,773,32768]";}
   byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });server.start();
  ModbusProperties p=new ModbusProperties();p.setIp("127.0.0.1");p.setPort(String.valueOf(server.getAddress().getPort()));p.setUsername("synthetic");p.setPassword("test-only");p.setLogName("HomePLC");client=new ModbusClientAdapter(p);
 }
 @AfterEach void stop(){server.stop(0);}
 private String read(InputStream input)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);}
 @Test void usesExistingApiAndPreservesLogReactionAndSource() throws Exception {
  response="[{\"oid\":9007199254740993,\"type\":16,\"level\":5,\"value\":0,\"prevValue\":3,\"devName\":\"HomePLC\",\"comment\":\"error count 0\"}]";
  SystemEventDto e=client.getEvents(240).get(0);assertEquals(Integer.valueOf(5),e.getLevel());assertEquals(Double.valueOf(0),e.getValue());assertEquals("HomePLC",e.getDevName());assertNull(e.getArrivalTime());assertEquals("9007199254740993",e.getOid());
  assertEquals("[16,3,4,6]",eventBodies.get(0));assertTrue(paths.contains("/api/ess2/events/list?lastminutes=240"));
 }
 @Test void renewsExpiredDataServerSessionOnceAndRereads() throws Exception{expireFirst=true;response="[{\"level\":3,\"type\":16}]";assertEquals(1,client.getEvents(60).size());assertEquals(2,events.get());assertEquals(2,logins.get());}
 @Test void nullResponseAndUpstreamFailureNeverBecomeHealthyEmptyList(){response="null";assertThrows(IOException.class,()->client.getEvents(60));status=503;response="{}";assertThrows(IOException.class,()->client.getEvents(60));assertEquals(2,events.get());}
 @Test void sendsThirtyAndNinetyDaysWithoutClamping() throws Exception {
  for(int minutes:new int[]{43200,129600}) {assertTrue(client.getEvents(minutes).isEmpty());assertTrue(paths.contains("/api/ess2/events/list?lastminutes="+minutes));}
 }
 @Test void emptyArrayIsAValidEmptyJournal() throws Exception{assertTrue(client.getEvents(60).isEmpty());}
 @Test void homeOverrideAppliesToReadWriteAndBatchButKeepsExplicitOtherUnits() throws Exception {
  assertEquals(42,client.readRegister(ModbusConstants.SAUNA_PLC,0x0301));client.writeRegister(ModbusConstants.SAUNA_PLC,0x0300,6);client.readRegisters(ModbusConstants.SAUNA_PLC,Arrays.asList(0x0301,0x0305));client.readRegister("OtherUnit",1);
  assertTrue(paths.stream().anyMatch(p->p.contains("logunit/read?logName=HomePLC&regnum=769")));
  assertTrue(paths.stream().anyMatch(p->p.contains("logunit/write?logName=HomePLC&regnum=768&value=6")));
  assertTrue(paths.stream().anyMatch(p->p.contains("list/logunit/read?logName=HomePLC")));
  assertTrue(paths.stream().anyMatch(p->p.contains("logName=OtherUnit&regnum=1")));
 }
}
