package com.example.modbus_api.service;

import com.example.modbus_api.dto.ClimateSensorDto;
import com.example.modbus_api.dto.DashboardData;
import com.example.modbus_api.dto.FanFlowData;
import com.example.modbus_api.dto.RecuperatorStatusDto;
import com.example.modbus_api.dto.SensorData;
import com.example.modbus_api.dto.TempSettingsDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReadingsLogService {

    private static final Logger log = LoggerFactory.getLogger(ReadingsLogService.class);
    private static final List<Integer> ALLOWED_INTERVALS = Arrays.asList(1, 5, 10, 30, 60);

    private static final String[][] COLUMNS = {
            {"state", "Состояние агрегата"},
            {"stateCode", "Код состояния"},
            {"enabled", "Агрегат включен"},
            {"bt3", "T1, °C"},
            {"bt11", "T2, °C"},
            {"bt10", "T3, °C"},
            {"bt1", "T4_1, °C"},
            {"bt2", "T5, °C"},
            {"bt21", "T4_2, °C"},
            {"bt8", "TH1, °C"},
            {"bt9", "TC1, °C"},
            {"bt24", "TH2, °C"},
            {"bt27", "TC2, °C"},
            {"bt3Humid", "T1 влажность, %"},
            {"bt11Humid", "T2 влажность, %"},
            {"bt10Humid", "T3 влажность, %"},
            {"bt1Humid", "T4_1 влажность, %"},
            {"bt2Humid", "T5 влажность, %"},
            {"bt21Humid", "T4_2 влажность, %"},
            {"bt8Humid", "TH1 влажность, %"},
            {"bt9Humid", "TC1 влажность, %"},
            {"bt24Humid", "TH2 влажность, %"},
            {"bt27Humid", "TC2 влажность, %"},
            {"targetZone1", "Целевая T зоны 1, °C"},
            {"targetZone2", "Целевая T зоны 2, °C"},
            {"bp1", "PF1, Па"},
            {"bp2", "PF2, Па"},
            {"bp5", "PR2, Па"},
            {"bp10", "PR1, Па"},
            {"bf11Pressure", "M1_1 давление, Па"},
            {"bf11Rpm", "M1_1 обороты, об/мин"},
            {"bf11Speed", "M1_1 скорость, %"},
            {"bf12Pressure", "M1_2 давление, Па"},
            {"bf12Rpm", "M1_2 обороты, об/мин"},
            {"bf12Speed", "M1_2 скорость, %"},
            {"bf21Pressure", "M2_1 давление, Па"},
            {"bf21Rpm", "M2_1 обороты, об/мин"},
            {"bf21Speed", "M2_1 скорость, %"},
            {"bf22Pressure", "M2_2 давление, Па"},
            {"bf22Rpm", "M2_2 обороты, об/мин"},
            {"bf22Speed", "M2_2 скорость, %"},
            {"vm1", "Расход притока, м³/ч"},
            {"vm2", "Расход вытяжки, м³/ч"},
            {"vh1", "TH1 · клапан нагрева, %"},
            {"vc1", "TC1 · клапан охлаждения, %"},
            {"vh2", "TH2 · клапан нагрева, %"},
            {"vc2", "TC2 · клапан охлаждения, %"},
            {"vhp1", "Вентиль ГВ зоны 1, %"},
            {"vcp1", "Вентиль ХВ зоны 1, %"},
            {"vop1", "Доп. открытие ГВ зоны 1, %"},
            {"vhp2", "Вентиль ГВ зоны 2, %"},
            {"vcp2", "Вентиль ХВ зоны 2, %"},
            {"vop2", "Доп. открытие ГВ зоны 2, %"},
            {"recEff", "КПД рекуператора, %"},
            {"recupTargetRpm", "Рекуператор целевые, об/мин"},
            {"recupCurrentRpm", "Рекуператор обороты вала, об/мин"},
            {"recupSpeedRpm", "Рекуператор скорость, об/мин"},
            {"recupTargetPercent", "Рекуператор цель, %"},
            {"recupTempBt6", "TTR · теплоноситель рекуператора, °C"},
            {"recupStateCode", "Код состояния рекуператора"},
            {"recupState", "Состояние рекуператора"},
            {"pumpActive", "Насос рекуператора"},
            {"damperSupply", "Задвижка притока"},
            {"damperExhaust", "Задвижка вытяжки"},
    };

    private final JdbcTemplate jdbc;
    private final DeviceService deviceService;
    private final Gson gson = new Gson();
    private volatile long lastSavedTs = 0;

    public ReadingsLogService(JdbcTemplate jdbc, DeviceService deviceService) {
        this.jdbc = jdbc;
        this.deviceService = deviceService;
    }

    @PostConstruct
    public void initSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS device_readings_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER NOT NULL, " +
                "data TEXT NOT NULL)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_device_readings_log_ts ON device_readings_log(ts)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS log_settings (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " +
                "readings_interval_min INTEGER NOT NULL)");
        jdbc.update("INSERT OR IGNORE INTO log_settings (id, readings_interval_min) VALUES (1, 5)");

        Long lastTs = jdbc.queryForObject("SELECT MAX(ts) FROM device_readings_log", Long.class);
        lastSavedTs = lastTs != null ? lastTs : 0;
    }

    public int getIntervalMinutes() {
        Integer interval = jdbc.queryForObject("SELECT readings_interval_min FROM log_settings WHERE id = 1", Integer.class);
        return interval != null ? interval : 5;
    }

    public void setIntervalMinutes(int minutes) {
        if (!ALLOWED_INTERVALS.contains(minutes)) {
            throw new IllegalArgumentException("Допустимые интервалы: 1, 5, 10, 30, 60 минут");
        }
        jdbc.update("UPDATE log_settings SET readings_interval_min = ? WHERE id = 1", minutes);
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void saveSnapshotIfDue() {
        long intervalMs = getIntervalMinutes() * 60_000L;
        long now = System.currentTimeMillis();
        if (now - lastSavedTs < intervalMs - 2_000) {
            return;
        }
        try {
            DashboardData data = deviceService.getDashboardData();
            jdbc.update("INSERT INTO device_readings_log (ts, data) VALUES (?, ?)", now, gson.toJson(flatten(data)));
            lastSavedTs = now;
        } catch (Exception e) {
            log.warn("Показания не записаны: {}", e.getMessage());
        }
    }

    public void writeXlsx(long from, long to, OutputStream out) throws IOException {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT ts, data FROM device_readings_log WHERE ts >= ? AND ts <= ? ORDER BY ts", from, to);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(200)) {
            Sheet sheet = workbook.createSheet("Показания");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));

            Row header = sheet.createRow(0);
            Cell tsHeader = header.createCell(0);
            tsHeader.setCellValue("Дата и время");
            tsHeader.setCellStyle(headerStyle);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = header.createCell(i + 1);
                cell.setCellValue(COLUMNS[i][1]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setColumnWidth(0, 20 * 256);
            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.setColumnWidth(i + 1, 18 * 256);
            }
            sheet.createFreezePane(1, 1);

            int rowNum = 1;
            for (Map<String, Object> dbRow : rows) {
                Row row = sheet.createRow(rowNum++);
                long ts = ((Number) dbRow.get("ts")).longValue();
                Cell tsCell = row.createCell(0);
                tsCell.setCellValue(new Date(ts));
                tsCell.setCellStyle(dateStyle);

                Map<String, Object> values = gson.fromJson(
                        String.valueOf(dbRow.get("data")),
                        new TypeToken<Map<String, Object>>() {}.getType());

                for (int i = 0; i < COLUMNS.length; i++) {
                    Object value = values != null ? values.get(COLUMNS[i][0]) : null;
                    if (value == null) {
                        continue;
                    }
                    Cell cell = row.createCell(i + 1);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value ? "Да" : "Нет");
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }

            workbook.write(out);
            workbook.dispose();
        }
    }

    private Map<String, Object> flatten(DashboardData d) {
        Map<String, Object> m = new LinkedHashMap<>();

        RecuperatorStatusDto status = d.getRecuperatorStatus();
        m.put("state", status != null ? status.getStateName() : null);
        m.put("stateCode", status != null ? status.getStateCode() : null);
        m.put("enabled", status != null ? status.isEnabled() : null);

        List<ClimateSensorDto> climate = d.getClimateSensors();
        String[] climateIds = {"BT3", "BT11", "BT10", "BT1", "BT2", "BT21", "BT8", "BT9", "BT24", "BT27"};
        for (String id : climateIds) {
            ClimateSensorDto sensor = findClimate(climate, id);
            m.put(id.toLowerCase(), sensor != null ? sensor.getTemperature() : null);
            m.put(id.toLowerCase() + "Humid", sensor != null ? sensor.getHumidity() : null);
        }

        TempSettingsDto tempSettings = d.getTempSettings();
        m.put("targetZone1", tempSettings != null ? tempSettings.getTargetZone1() : null);
        m.put("targetZone2", tempSettings != null ? tempSettings.getTargetZone2() : null);

        m.put("bp1", parseNumber(findSensor(d.getWSensors(), "BP1")));
        m.put("bp2", parseNumber(findSensor(d.getWSensors(), "BP2")));
        m.put("bp5", parseNumber(findSensor(d.getPressureSensors(), "BP5")));
        m.put("bp10", parseNumber(findSensor(d.getPressureSensors(), "BP10")));

        List<FanFlowData> fans = d.getFans();
        String[] fanKeys = {"bf11", "bf12", "bf21", "bf22"};
        for (int i = 0; i < fanKeys.length; i++) {
            FanFlowData fan = fans != null && fans.size() > i ? fans.get(i) : null;
            m.put(fanKeys[i] + "Pressure", fan != null ? fan.getPressure() : null);
            m.put(fanKeys[i] + "Rpm", fan != null ? fan.getVm() : null);
            m.put(fanKeys[i] + "Speed", fan != null ? fan.getSpeedPerc() : null);
        }

        m.put("vm1", d.getVm1());
        m.put("vm2", d.getVm2());

        String[] valveIds = {"VH1", "VC1", "VH2", "VC2"};
        for (String id : valveIds) {
            m.put(id.toLowerCase(), parseNumber(findSensor(d.getValves(), id)));
        }
        String[] debugValveIds = {"VHP1", "VCP1", "VOP1", "VHP2", "VCP2", "VOP2"};
        for (String id : debugValveIds) {
            m.put(id.toLowerCase(), parseNumber(findSensor(d.getDebugValveSensors(), id)));
        }

        m.put("recEff", parseNumber(findSensor(d.getRecuperator(), "R_EFFICIENCY")));
        m.put("recupTargetRpm", d.getRecupTargetRpm());
        m.put("recupCurrentRpm", d.getRecupCurrentRpm());
        m.put("recupSpeedRpm", d.getRecupSpeedRpm());
        m.put("recupTargetPercent", d.getRecupTargetPercent());
        m.put("recupTempBt6", d.getRecupTempBt6());
        m.put("recupStateCode", d.getRecupStateCode());
        m.put("recupState", d.getRecupStateName());
        m.put("pumpActive", d.getPumpActive());

        m.put("damperSupply", d.getDamperSupply());
        m.put("damperExhaust", d.getDamperExhaust());

        return m;
    }

    private static ClimateSensorDto findClimate(List<ClimateSensorDto> sensors, String id) {
        if (sensors == null) {
            return null;
        }
        for (ClimateSensorDto sensor : sensors) {
            if (id.equals(sensor.getId())) {
                return sensor;
            }
        }
        return null;
    }

    private static SensorData findSensor(List<SensorData> sensors, String id) {
        if (sensors == null) {
            return null;
        }
        for (SensorData sensor : sensors) {
            if (id.equals(sensor.getId())) {
                return sensor;
            }
        }
        return null;
    }

    private static Double parseNumber(SensorData sensor) {
        if (sensor == null || sensor.getValue() == null) {
            return null;
        }
        try {
            return Double.parseDouble(sensor.getValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
