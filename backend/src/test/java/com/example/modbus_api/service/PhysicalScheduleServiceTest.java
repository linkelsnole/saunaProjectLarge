package com.example.modbus_api.service;
import com.example.modbus_api.dto.PhysicalScheduleDto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
class PhysicalScheduleServiceTest {
    ModbusClientAdapter client;
    PhysicalScheduleService service;
    Map<Integer,Integer> memory;
    int base=ModbusRegisterMap.scheduleBaseById(7);
    @BeforeEach void setup() throws Exception {
        client=mock(ModbusClientAdapter.class);service=new PhysicalScheduleService(client);memory=new HashMap<>();
        for(int i=0;i<18;i++) memory.put(base+i,0);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenAnswer(a->new HashMap<>(memory));
        when(client.readRegister(eq(SAUNA_PLC),anyInt())).thenAnswer(a->memory.getOrDefault(a.getArgument(1),0));
        doAnswer(a->{memory.put(a.getArgument(1),a.getArgument(2));return null;}).when(client).writeRegister(eq(SAUNA_PLC),anyInt(),anyInt());
    }
    Update patch(Change...changes) {return new Update(Arrays.asList(changes),true);}
    @Test void rawReadsRetainUnknownValuesAndMissingRegisters() throws Exception {
        memory.put(base,99);memory.remove(base+12);Snapshot all=service.readAll();
        assertEquals(20,all.getSlots().size());assertEquals(Integer.valueOf(99),all.getSlots().get(6).getValues().get(0));
        assertNull(all.getSlots().get(6).getValues().get(12));assertEquals(0x2260,all.getSlots().get(19).getBaseAddress());
        verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }
    @Test void groupAndManualModesAreWrittenExactlyWithoutAutoOverrides() throws Exception {
        Result result=service.update(7,patch(new Change(8,0,42),new Change(12,0,3),new Change(15,0,1)));assertTrue(result.isVerified());
        org.mockito.InOrder order=inOrder(client);
        order.verify(client).writeRegister(SAUNA_PLC,base+8,42);order.verify(client).writeRegister(SAUNA_PLC,base+12,3);
        order.verify(client).writeRegister(SAUNA_PLC,base+15,1);order.verify(client).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
        verify(client,times(4)).writeRegister(anyString(),anyInt(),anyInt());
    }
    @Test void selectedPreservesFreshWorkingEvenWhenItChangedSinceTheFormOpened() throws Exception {
        memory.put(base+1,2);assertTrue(service.update(7,patch(new Change(1,0,1))).isVerified());
        verify(client).writeRegister(SAUNA_PLC,base+1,3);
    }
    @Test void cannotWriteWorkingDirectly() {
        assertThrows(IllegalArgumentException.class,()->service.update(7,patch(new Change(1,0,3))));verifyNoInteractions(client);
    }
    @Test void concurrentEditRejectsEntirePatchBeforeAnyWrites() throws Exception {
        memory.put(base+8,10);assertThrows(ConcurrentModificationException.class,()->service.update(7,patch(new Change(12,0,2),new Change(8,0,5))));
        verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }
    @Test void validatesTimeDateEnumsAndFloatBeforeAnyIo() {
        for(Change change:Arrays.asList(new Change(5,0,0x1830),new Change(3,0,0x0230),new Change(12,0,4),new Change(10,0,1),new Change(18,0,1)))
            assertThrows(IllegalArgumentException.class,()->service.update(7,patch(change)));
        assertThrows(IllegalArgumentException.class,()->service.update(7,patch(new Change(10,0,0),new Change(11,0,0x7F80))));verifyNoInteractions(client);
    }
    @Test void writesFloatLowWordFirstAndAcceptsNegativeTemperature() throws Exception {
        float target=1234.5f;
        Result result=service.update(7,patch(new Change(10,0,ModbusRegisterMap.floatLowWord(target)),new Change(11,0,ModbusRegisterMap.floatHighWord(target)),new Change(14,0,0xFFCE)));
        assertTrue(result.isVerified());assertEquals(Double.valueOf(1234.5),ModbusRegisterMap.decodeFloat(memory.get(base+10),memory.get(base+11)));
    }
    @Test void partialFailureDoesNotApplyAndIncludesAlreadySentAddress() throws Exception {
        doThrow(new IOException("connection lost")).when(client).writeRegister(SAUNA_PLC,base+15,2);
        IOException error=assertThrows(IOException.class,()->service.update(7,patch(new Change(12,0,2),new Change(15,0,2))));
        assertTrue(error.getMessage().contains("0x20CC"));verify(client,never()).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
    }
    @Test void readbackMismatchIsNotReportedAsVerified() throws Exception {
        doNothing().when(client).writeRegister(SAUNA_PLC,base+8,77);assertFalse(service.update(7,patch(new Change(8,0,77))).isVerified());
    }
    @Test void missingTouchedRegisterPreventsAnyWrite() throws Exception {
        memory.remove(base+12);assertThrows(IOException.class,()->service.update(7,patch(new Change(12,0,2))));verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }
}
