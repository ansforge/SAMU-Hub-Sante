/**
 * Copyright © 2023-2026 Agence du Numerique en Sante (ANS)
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
package com.hubsante.hub.config;

import com.hubsante.hub.service.ClientPropertiesRegistry;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class HubConfiguration {

    private static final String DATA_DIVIDER = ",";
    private static final String COLUMN_DIVIDER = ";";

    @Value("${supported.messages.file}")
    private File supportedMessagesFile;

    @Value("${dispatcher.default.ttl}")
    private String ttlProperty;

    private long defaultTTL;

    @Value("${spring.rabbitmq.virtual-host}")
    private String vhost;

    @Autowired private ClientPropertiesRegistry clientPropertiesRegistry;

    private List<String> supportedMessages;

    @PostConstruct
    public void init() throws Exception {
        // We first get the parameterized default message TTL
        defaultTTL = Long.parseLong(this.ttlProperty);

        // We explicitly set the Locale to ensure cross platform consistency
        Locale.setDefault(Locale.ENGLISH);

        supportedMessages = loadSupportedMessages(vhost);
    }

    public List<String> loadSupportedMessages(String vhost) throws Exception {
        List<String> supportedMessages = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new FileReader(supportedMessagesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("common" + COLUMN_DIVIDER)
                        || line.startsWith(vhost + COLUMN_DIVIDER)) {
                    String[] rowParts = line.split(COLUMN_DIVIDER);
                    if (rowParts.length > 1) {
                        String[] messages = rowParts[1].split(DATA_DIVIDER);
                        for (String messageClassName : messages) {
                            String messageClassNameTrimmed = messageClassName.trim();
                            if (!messageClassNameTrimmed.isEmpty()
                                    && !supportedMessages.contains(messageClassNameTrimmed)) {
                                supportedMessages.add(messageClassNameTrimmed);
                            }
                        }
                    }
                }
            }
            ;
        } catch (IOException e) {
            throw new Exception("Error reading supported messages file: {}", e);
        }
        return supportedMessages;
    }

    public List<String> getSupportedMessages() {
        return supportedMessages;
    }

    public long getDefaultTTL() {
        return defaultTTL;
    }

    public String getVhost() {
        return vhost;
    }

    public ClientPropertiesRegistry getClientPropertiesRegistry() {
        return clientPropertiesRegistry;
    }

    @Bean
    public EdxlHandler edxlHandler() {
        return new EdxlHandler();
    }

    @Bean
    public Validator validator() {
        return new Validator();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public WebClient conversionWebClient(
            ObservationRegistry observationRegistry,
            @Value("${conversion.service.url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .observationRegistry(observationRegistry)
                .build();
    }
}
