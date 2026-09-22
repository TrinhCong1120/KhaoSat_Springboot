package com.trinhcong1120.gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.config.import=",
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false"
})
class GatewayServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
