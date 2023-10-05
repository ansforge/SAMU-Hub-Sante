package com.hubsante.hub.config;

import org.junit.jupiter.params.shadow.com.univocity.parsers.common.ParsingContext;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.processor.ObjectRowProcessor;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParser;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParserSettings;
import org.springframework.beans.factory.annotation.Value;
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
}
