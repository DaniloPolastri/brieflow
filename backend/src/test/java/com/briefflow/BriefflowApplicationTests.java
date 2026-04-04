package com.briefflow;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running PostgreSQL - will be enabled with Testcontainers")
class BriefflowApplicationTests {

	@Test
	void contextLoads() {
	}

}
