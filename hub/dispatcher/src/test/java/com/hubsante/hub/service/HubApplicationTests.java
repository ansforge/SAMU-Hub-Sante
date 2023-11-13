/**
 * Copyright © 2023-2023 Agence du Numerique en Sante (ANS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubsante.hub.service;

import com.hubsante.hub.HubApplication;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class, initializers = HubApplicationTests.Initializer.class)
class HubApplicationTests {
	@Test
	void contextLoads() {
	}

	public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			val values = TestPropertyValues.of(

					// default RabbitTemplate conf (dispatcher)
					"spring.rabbitmq.ssl.key-store-password=dispatcher",
					"spring.rabbitmq.ssl.trust-store-password=trustStore",
					"spring.rabbitmq.ssl.key-store=" + Thread.currentThread().getContextClassLoader()
							.getResource("config/certs/dispatcher/dispatcher.test.p12"),
					"spring.rabbitmq.ssl.trust-store=" + Thread.currentThread().getContextClassLoader()
							.getResource("config/certs/trustStore"),
					"client.preferences.file=" + Thread.currentThread().getContextClassLoader()
							.getResource("config/client.preferences.csv")
			);
			values.applyTo(applicationContext);
		}
	}

}
