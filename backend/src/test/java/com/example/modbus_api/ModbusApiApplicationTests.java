package com.example.modbus_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-context-large.db")
class ModbusApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
