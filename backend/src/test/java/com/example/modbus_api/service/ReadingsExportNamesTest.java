package com.example.modbus_api.service;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ReadingsExportNamesTest {
 @Test void exportsCurrentNamesAndRetainsHistoricalValues() throws Exception {
  JdbcTemplate jdbc=mock(JdbcTemplate.class);
  Map<String,Object> historical=new HashMap<>(); historical.put("ts",1L);
  historical.put("data","{\"bt11\":21.5,\"bp10\":93,\"bf11Pressure\":42}");
  when(jdbc.queryForList(anyString(),eq(0L),eq(10L))).thenReturn(Collections.singletonList(historical));
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  new ReadingsLogService(jdbc,mock(DeviceService.class)).writeXlsx(0,10,out);
  try(XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
   org.apache.poi.ss.usermodel.Sheet sheet=book.getSheetAt(0);Map<String,Integer> columns=new HashMap<>();
   for(org.apache.poi.ss.usermodel.Cell cell:sheet.getRow(0)) {
    String label=cell.getStringCellValue();columns.put(label,cell.getColumnIndex());
    assertFalse(label.matches(".*\\b(BT[0-9]+|BP[0-9]+|BF[0-9]+)\\b.*"),label);
   }
   assertEquals(21.5,sheet.getRow(1).getCell(columns.get("T2, °C")).getNumericCellValue());
   assertEquals(93,sheet.getRow(1).getCell(columns.get("PR1, Па")).getNumericCellValue());
   assertEquals(42,sheet.getRow(1).getCell(columns.get("M1_1 давление, Па")).getNumericCellValue());
  }
 }
}
