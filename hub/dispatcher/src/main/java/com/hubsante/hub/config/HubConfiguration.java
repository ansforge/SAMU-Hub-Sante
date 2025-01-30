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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

@Configuration
public class HubConfiguration {

    private static final int TOGGLE_ROW_LENGTH = 3;

    @Value("${client.preferences.file}")
    private File configFile;
    @Value("${dispatcher.default.ttl}")
    private String ttlProperty;
    private long defaultTTL;
    @Value("${dispatcher.vhost}")
    private String vhost;

    private HashMap<String, Boolean> useXmlPreferences = new HashMap<>();
    private HashMap<String, Boolean> directCisuPreferences = new HashMap<>();

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
            ObjectRowProcessor rowProcessor = new ObjectRowProcessor() {
                @Override
                public void rowProcessed(Object[] objects, ParsingContext parsingContext) {
                    if (objects.length != TOGGLE_ROW_LENGTH) {
                        throw new IllegalArgumentException();
                    }
                    String[] items = Arrays.asList(objects).toArray(new String[TOGGLE_ROW_LENGTH]);
                    useXmlPreferences.put(items[0], Boolean.parseBoolean(items[1]));
                    directCisuPreferences.put(items[0], Boolean.parseBoolean(items[2]));
                }
            };
            CsvParserSettings parserSettings = new CsvParserSettings();
            parserSettings.getFormat().setLineSeparator("\n");
            parserSettings.getFormat().setDelimiter(';');
            parserSettings.setProcessor(rowProcessor);
            parserSettings.setHeaderExtractionEnabled(true);
            parserSettings.setNullValue("");

            CsvParser parser = new CsvParser(parserSettings);
            parser.parse(new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new Exception("Could not read config file " + configFile.getAbsolutePath(), e);
        }
    }

    public HashMap<String, Boolean> getUseXmlPreferences() {
        return useXmlPreferences;
    }

    public HashMap<String, Boolean> getDirectCisuPreferences() {
        return directCisuPreferences;
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

    @Bean
    public WebClient conversionWebClient(@Value("${conversion.service.url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
