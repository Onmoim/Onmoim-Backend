package com.onmoim.server;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InitTest {
	@Autowired
	DataSource dataSource;
	@Test
	void setGlobalMaxConnections() throws Exception {
		dataSource.getConnection()
			.createStatement().execute("SET GLOBAL max_connections = 200");
	}
}
