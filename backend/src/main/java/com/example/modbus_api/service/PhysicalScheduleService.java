package com.example.modbus_api.service;

import com.example.modbus_api.dto.PhysicalScheduleDto.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.MonthDay;
import java.util.*;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;

@Service
public class PhysicalScheduleService {
    private final ModbusClientAdapter client;
    public PhysicalScheduleService(ModbusClientAdapter client) { this.client = client; }

    public Snapshot readAll() throws IOException {
        List<Integer> addresses = new ArrayList<>();
        addresses.add(ModbusRegisterMap.SCHEDULE_SIZE);
        for (int i=1; i<=20; i++) addresses.addAll(addresses(i));
        Map<Integer,Integer> raw = client.readRegisters(SAUNA_PLC, addresses);
        List<Slot> slots = new ArrayList<>();
        for (int i=1; i<=20; i++) slots.add(slot(i,raw));
        return new Snapshot(slots,raw.get(ModbusRegisterMap.SCHEDULE_SIZE),System.currentTimeMillis());
    }

    public Slot read(int id) throws IOException {
        return slot(id,client.readRegisters(SAUNA_PLC,addresses(id)));
    }

    // Configuration patches affect only explicit fields. Working is always preserved.
    public synchronized Result update(int id, Update request) throws IOException {
        int base=ModbusRegisterMap.scheduleBaseById(id);
        List<Change> changes=validate(request);
        Map<Integer,Integer> current=client.readRegisters(SAUNA_PLC,addresses(id));
        for (Change c:changes) {
            Integer actual=current.get(base+c.getOffset());
            if (actual==null) throw new IOException("Регистр недоступен: "+hex(base+c.getOffset()));
            int mask=c.getOffset()==1 ? 1 : 0xFFFF;
            if ((actual & mask)!=(c.getExpected() & mask)) {
                throw new ConcurrentModificationException("Значение "+hex(base+c.getOffset())+" изменилось. Перечитайте слот перед сохранением.");
            }
        }
        List<String> sent=new ArrayList<>();
        try {
            for (Change c:changes) {
                int value=c.getValue();
                if (c.getOffset()==1) {
                    // Re-read immediately before a bit update; never write back a stale Working bit.
                    int live=client.readRegister(SAUNA_PLC,base+1);
                    if (live==ModbusConstants.INVALID_VALUE) throw new IOException("Состояние слота недоступно");
                    if ((live&1)!=(c.getExpected()&1)) throw new IOException("Selected изменился во время записи");
                    value=(live & ~1) | value;
                }
                client.writeRegister(SAUNA_PLC,base+c.getOffset(),value);
                sent.add(hex(base+c.getOffset()));
            }
            if(request.isApply()) client.writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,ModbusConstants.CMD_FAST_MODE_CHANGE);
            Slot result=read(id);
            boolean verified=true;
            for(Change c:changes) {
                Integer actual=result.getValues().get(c.getOffset());
                int mask=c.getOffset()==1 ? 1 : 0xFFFF;
                if(actual==null || (actual&mask)!=(c.getValue()&mask)) verified=false;
            }
            return new Result(result,verified);
        } catch(IOException e) {
            throw new IOException("Запись не подтверждена полностью. Уже отправлены: "+sent+". Перечитайте слот. "+e.getMessage(),e);
        }
    }

    public synchronized void apply() throws IOException {
        client.writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,ModbusConstants.CMD_FAST_MODE_CHANGE);
    }

    private List<Change> validate(Update request) {
        if(request==null || request.getChanges()==null || request.getChanges().isEmpty() || request.getChanges().size()>18)
            throw new IllegalArgumentException("Выберите поля для записи");
        Map<Integer,Change> fields=new TreeMap<>();
        for(Change c:request.getChanges()) {
            if(c==null || c.getOffset()==null || c.getExpected()==null || c.getValue()==null) throw new IllegalArgumentException("Неполное изменение регистра");
            int o=c.getOffset(),v=c.getValue();
            if(o<0 || o>17 || fields.put(o,c)!=null) throw new IllegalArgumentException("Недопустимое или повторяющееся смещение");
            if(v<0 || v>65535 || c.getExpected()<0 || c.getExpected()>65535) throw new IllegalArgumentException("Значение регистра должно быть 0–65535");
            if((o==0 && v>5)||(o==1 && v>1)||(o==2 && v>127)||(o==9 && v>4)||((o==12||o==15)&&v>3)||((o==13||o==16)&&v>2))
                throw new IllegalArgumentException("Значение не соответствует карте регистра +"+o);
            if(o==3||o==4) {
                try { MonthDay.of(v>>8,v&255); }
                catch(java.time.DateTimeException e) { throw new IllegalArgumentException("Дата должна содержать месяц и день"); }
            }
            if((o==5||o==6) && ((v>>8)>23 || (v&255)>59)) throw new IllegalArgumentException("Время должно быть в формате часы:минуты");
        }
        if(fields.containsKey(10)!=fields.containsKey(11)) throw new IllegalArgumentException("Объём записывается двумя словами float одновременно");
        if(fields.containsKey(10)) {
            Double airflow=ModbusRegisterMap.decodeFloat(fields.get(10).getValue(),fields.get(11).getValue());
            if(airflow==null || airflow<0) throw new IllegalArgumentException("Объём должен быть конечным неотрицательным числом");
        }
        List<Change> ordered=new ArrayList<>(fields.values());
        ordered.sort(Comparator.comparingInt(c->c.getOffset()==1?100:c.getOffset()));
        return ordered;
    }
    private List<Integer> addresses(int id) {
        int base=ModbusRegisterMap.scheduleBaseById(id);
        List<Integer> result=new ArrayList<>();
        for(int o=0;o<18;o++) result.add(base+o);
        return result;
    }
    private Slot slot(int id,Map<Integer,Integer> values) {
        int base=ModbusRegisterMap.scheduleBaseById(id);
        List<Integer> result=new ArrayList<>();
        for(int o=0;o<18;o++) result.add(values.get(base+o));
        return new Slot(id,base,result);
    }
    private String hex(int value) { return String.format("0x%04X",value); }
}
