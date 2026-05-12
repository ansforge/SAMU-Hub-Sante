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

import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.ObjectRowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class HubConfiguration {

    private static final int ROW_LENGTH = 11;
    private static final String DATA_DIVIDER = ",";
    private static final String COLUMN_DIVIDER = ";";
    private static final String CLIENT_ID_HEADER = "client_id";
    private static final String INHIBITED_USE_CASES_HEADER = "inhibited_use_cases";

    private static final StructuredLogger structuredLog = new StructuredLogger(log);

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
    private Map<String, List<String>> clientsInhibitedMessages = new HashMap<>();
    private List<String> supportedMessages;

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
            ObjectRowProcessor clientPreferencesRowProcessor =
                    new ObjectRowProcessor() {
                        @Override
                        public void rowProcessed(Object[] objects, ParsingContext parsingContext) {
                            if (objects.length != ROW_LENGTH) {
                                log.warn(
                                        "There were more than {} columns in the client preferences file, extra columns are being ignored",
                                        ROW_LENGTH);
                            }
                            String[] items = Arrays.asList(objects).toArray(new String[ROW_LENGTH]);
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
            clientsInhibitedMessages = loadClientsInhibitedMessages();
            supportedMessages = loadSupportedMessages(vhost);
        } catch (Exception e) {
            throw new Exception("Could not read config file " + configFile.getAbsolutePath(), e);
        }
    }

    public Map<String, Map<String, String>> loadClientsPerimetersAndVersions() throws IOException {
        Map<String, Map<String, String>> clientsPerimeterAndVersions = new HashMap<>();
        BufferedReader reader =
                new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        String[] headers = headerLine.split(COLUMN_DIVIDER);
        int numberOfColumns = headers.length;

        Set<String> perimeterNames =
                Arrays.stream(Constants.Perimeter.values())
                        .map(Constants.Perimeter::getName)
                        .collect(Collectors.toSet());

        Map<String, Integer> perimeterColumnIndexes = new HashMap<>();
        for (int i = 0; i < numberOfColumns; i++) {
            if (perimeterNames.contains(headers[i])) {
                perimeterColumnIndexes.put(headers[i], i);
            }
        }
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(COLUMN_DIVIDER, -1); // -1 allows trailing empty strings

            if (values.length < numberOfColumns) continue;

            String clientId = values[0];
            Map<String, String> allPerimetersVersions = new HashMap<>();

            for (Map.Entry<String, Integer> perimeterMatch : perimeterColumnIndexes.entrySet()) {
                String perimeterName = perimeterMatch.getKey();
                int columnIndex = perimeterMatch.getValue();
                allPerimetersVersions.put(perimeterName, values[columnIndex]);
            }

            clientsPerimeterAndVersions.put(clientId, allPerimetersVersions);
        }

        reader.close();
        return clientsPerimeterAndVersions;
    }

    public String[] getClientVersionsForPerimeter(String clientId, String perimeterName) {
        Map<String, String> clientPerimeterDefinition =
                clientsPerimeterAndVersions.getOrDefault(clientId, null);
        if (clientPerimeterDefinition == null) {
            structuredLog.warn(
                    "ClientId was not found in clientsPerimeterAndVersions, or the variable is not initialized.",
                    Map.of(LogConstants.RECIPIENT_ID, clientId));
            return null;
        }
        String versions = clientPerimeterDefinition.getOrDefault(perimeterName, null);
        return splitString(versions);
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

    private Map<String, List<String>> loadClientsInhibitedMessages() throws IOException {
        Map<String, List<String>> result = new HashMap<>();

        try (Reader reader = Files.newBufferedReader(configFile.toPath());
                CSVParser parser =
                        CSVFormat.DEFAULT
                                .builder()
                                .setDelimiter(';')
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setTrim(true)
                                .build()
                                .parse(reader)) {
            boolean hasUseCasesColumn =
                    parser.getHeaderMap().containsKey(INHIBITED_USE_CASES_HEADER);

            for (CSVRecord record : parser) {

                String clientId = record.get(CLIENT_ID_HEADER);
                List<String> useCases;

                if (hasUseCasesColumn) {
                    useCases =
                            Arrays.stream(record.get(INHIBITED_USE_CASES_HEADER).split(","))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .toList();
                } else {
                    useCases = List.of();
                }

                result.put(clientId, useCases);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    public List<String> getSupportedMessages() {
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

    public Map<String, List<String>> getClientsInhibitedMessages() {
        return clientsInhibitedMessages;
    }

    public long getDefaultTTL() {
        return defaultTTL;
    }

    public String getVhost() {
        return vhost;
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

    public static String[] splitString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return input.split(DATA_DIVIDER);
    }

    @Bean
    public WebClient conversionWebClient(@Value("${conversion.service.url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
