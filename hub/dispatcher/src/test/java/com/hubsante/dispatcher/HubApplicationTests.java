package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
// You should change the active profile to test it locally
@ActiveProfiles({"local","bbo"})
class HubApplicationTests {

	@Test
	void contextLoads() {
	}

}
