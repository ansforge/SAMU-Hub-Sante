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
package com.hubsante.hub.config;

import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.ParsingContext;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.processor.ObjectRowProcessor;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParser;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParserSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

@Configuration
public class HubConfiguration {

    private static final int TOGGLE_ROW_LENGTH = 2;

    @Value("${client.preferences.file}")
    private File configFile;
    @Value("${hubsante.default.message.ttl}")
    private String ttlProperty;
    private long defaultTTL;

    private HashMap<String, Boolean> clientPreferences = new HashMap<>();

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
                    clientPreferences.put(items[0], Boolean.parseBoolean(items[1]));
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

    public HashMap<String, Boolean> getClientPreferences() {
        return clientPreferences;
    }

    public long getDefaultTTL() {
        return defaultTTL;
    }

    @Bean
    public EdxlHandler edxlHandler() {
        return new EdxlHandler();
    }

    @Bean
    public Validator validator() {
        return new Validator();
    }
}
