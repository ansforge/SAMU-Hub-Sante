/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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

import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.ParsingContext;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.processor.ObjectRowProcessor;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParser;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParserSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Configuration
public class HubConfiguration {

    private static final int TOGGLE_ROW_LENGTH = 9;
    private static final String DATA_DIVIDER = ",";

    @Value("${client.preferences.file}")
    private File configFile;

    @Value("${supported.messages.file}")
    private File supportedMessagesFile;

    @Value("${dispatcher.default.ttl}")
    private String ttlProperty;

    private long defaultTTL;

    @Value("${spring.rabbitmq.virtual-host}")
    private String vhost;

    private HashMap<String, Boolean> useXmlPreferences = new HashMap<>();
    private HashMap<String, Boolean> directCisuPreferences = new HashMap<>();
    private HashMap<String, String> clientsEditorMap = new HashMap<>();
    private Map<String, Map<String, String>> clientsPerimeterAndVersions = new HashMap<>();

    @PostConstruct
    public void init() throws Exception {

        try {
            // We first get the parameterized default message TTL
            defaultTTL = Long.parseLong(this.ttlProperty);

            // We explicitly set the Locale to ensure cross platform consistency
            Locale.setDefault(Locale.ENGLISH);

            // We define a custom row processor to read the config file
            // we override the rowProcessed method on the fly to store the config in a HashMap
            // then we define the parser settings and parse the file
            ObjectRowProcessor clientPreferencesRowProcessor = new ObjectRowProcessor() {
                @Override
                public void rowProcessed(Object[] objects, ParsingContext parsingContext) {
                    if (objects.length != TOGGLE_ROW_LENGTH) {
                        log.warn("There were more than {} columns in the client preferences file, extra columns are being ignored", TOGGLE_ROW_LENGTH);
                    }
                    String[] items = Arrays.asList(objects).toArray(new String[TOGGLE_ROW_LENGTH]);
                    useXmlPreferences.put(items[0], Boolean.parseBoolean(items[1]));
                    directCisuPreferences.put(items[0], Boolean.parseBoolean(items[2]));
                    clientsEditorMap.put(items[0], items[3]);
                }
            };
            CsvParserSettings parserSettings = new CsvParserSettings();
            parserSettings.getFormat().setLineSeparator("\n");
            parserSettings.getFormat().setDelimiter(';');
            parserSettings.setHeaderExtractionEnabled(true);
            parserSettings.setNullValue("");
            parserSettings.setProcessor(clientPreferencesRowProcessor);

            CsvParser parser = new CsvParser(parserSettings);
            parser.parse(new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8)));
            clientsPerimeterAndVersions = loadClientsPerimetersAndVersions();
        } catch (Exception e) {
            throw new Exception("Could not read config file " + configFile.getAbsolutePath(), e);
        }
    }

    public Map<String, Map<String, String>> loadClientsPerimetersAndVersions() throws IOException {
        String COLUMN_DIVIDER = ";";
        int NUMBER_OF_PERIMETERS = 4;
        Map<String, Map<String, String>> clientsPerimeterAndVersions = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8));
        String line;
        String headerLine = reader.readLine();
        String[] headers = headerLine.split(COLUMN_DIVIDER);
        int numberOfColumns = headers.length;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(COLUMN_DIVIDER);

            if (values.length < numberOfColumns) continue;

            String clientId = values[0];
            Map<String, String> allPerimetersVersions = new HashMap<>();

            for (int i = numberOfColumns - NUMBER_OF_PERIMETERS; i < numberOfColumns; i++) {
                allPerimetersVersions.put(headers[i], values[i]);
            }

            clientsPerimeterAndVersions.put(clientId, allPerimetersVersions);
        }

        reader.close();
        return clientsPerimeterAndVersions;
    }

    public String[] getClientVersionsForPerimeter(String clientId, String perimeterName) {
        Map<String, String> clientPerimeterDefinition = clientsPerimeterAndVersions.getOrDefault(clientId, null);
        String versions = clientPerimeterDefinition.getOrDefault(perimeterName, null);
        return splitString(versions);
    }

    public List<String> getSupportedMessages(String vhost) throws Exception{
        List<String> supportedMessages = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(supportedMessagesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("common" + DATA_DIVIDER) || line.startsWith(vhost + DATA_DIVIDER)) {
                    String[] rowParts = line.split(DATA_DIVIDER);
                    if (rowParts.length > 1) {
                        String[] messages = rowParts[1].split(";");
                        for (String messageClassName : messages) {
                            String messageClassNameTrimmed = messageClassName.trim();
                            if (!messageClassNameTrimmed.isEmpty() && !supportedMessages.contains(messageClassNameTrimmed)) {
                                supportedMessages.add(messageClassNameTrimmed);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new Exception("Error reading supported messages file: {}", e);
        }
        return supportedMessages;
    }

    public HashMap<String, Boolean> getUseXmlPreferences() {
        return useXmlPreferences;
    }

    public HashMap<String, Boolean> getDirectCisuPreferences() {
        return directCisuPreferences;
    }

    public HashMap<String, String> getClientsEditorMap() {
        return clientsEditorMap;
    }

    public long getDefaultTTL() {
        return defaultTTL;
    }

    public String getVhost() {return vhost; }

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

    public static String[] splitString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return input.split(DATA_DIVIDER);
    }

    @Bean
    public WebClient conversionWebClient(@Value("${conversion.service.url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
