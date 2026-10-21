package com.example.modbus_api.service;
import com.example.modbus_api.config.ModbusProperties;
import com.example.modbus_api.dto.SystemEventDto;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ControllerDiagnosticsServiceTest {
 @Test void wireModelNeverInventsSeverityDatesOrRegisterZero(){SystemEventDto event=new Gson().fromJson("{\"comment\":\"test\"}",SystemEventDto.class);assertNull(event.getLevel());assertNull(event.getArrivalTime());assertNull(event.getEndTime());assertNull(event.getRegNum());assertNull(event.getValue());}
 @Test void retainsLargeIdsAndUnknownSeverity(){SystemEventDto event=new Gson().fromJson("{\"oid\":9007199254740993,\"level\":99,\"arrivalTime\":{\"timeInMS\":1790000000000}}",SystemEventDto.class);assertEquals("9007199254740993",event.getOid());assertEquals(Integer.valueOf(99),event.getLevel());assertEquals(Long.valueOf(1790000000000L),event.getArrivalTime().getTimeInMS());}
 @Test void readsOnlyDocumentedAddressesAndPreservesCrashBit() throws Exception {
  ModbusClientAdapter client=mock(ModbusClientAdapter.class);ModbusProperties config=new ModbusProperties();config.setLogName("HomePLC");Map<Integer,Integer> raw=new HashMap<>();raw.put(0x0301,0);raw.put(0x0305,0x8000);raw.put(0x0600,17);raw.put(0x0302,8);raw.put(0x0306,32);
  when(client.readRegisters(eq(ModbusConstants.SAUNA_PLC),anyList())).thenReturn(raw);
  ControllerDiagnosticsService.Snapshot data=new ControllerDiagnosticsService(client,config).read();
  assertEquals(Integer.valueOf(8),data.getState2());assertEquals(Integer.valueOf(32),data.getWorkState2());assertEquals("HomePLC",data.getLogName());assertEquals(Integer.valueOf(0x8000),data.getWorkState());assertEquals(Integer.valueOf(17),data.getModbusErrors());assertNull(data.getErrorRestarts());
  verify(client).readRegisters(ModbusConstants.SAUNA_PLC,Arrays.asList(0x0301,0x0302,0x0305,0x0306,0x0600,0x0604,0x0605));verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
 }
 @Test void missingWordsStayUnknownRatherThanHealthyZero() throws Exception {ModbusClientAdapter client=mock(ModbusClientAdapter.class);when(client.readRegisters(anyString(),anyList())).thenReturn(Collections.emptyMap());ControllerDiagnosticsService.Snapshot s=new ControllerDiagnosticsService(client,new ModbusProperties()).read();assertNull(s.getState2());assertNull(s.getWorkState2());assertNull(s.getState());assertNull(s.getWorkState());assertNull(s.getModbusErrors());}
 @Test void unitDefaultsToSaunaAndAllowsHomeOverride(){ModbusProperties config=new ModbusProperties();assertEquals("Sauna21000PLC",config.getLogName());config.setLogName("HomePLC");assertEquals("HomePLC",config.getLogName());assertThrows(IllegalArgumentException.class,()->config.setLogName(""));}
}
