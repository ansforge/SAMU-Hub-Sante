package com.hubsante;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.edxl.EdxlMessage;
import io.github.cdimascio.dotenv.Dotenv;

import static com.hubsante.Utils.getRouting;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProducerRun {
    private static final String TLS_PROTOCOL_VERSION = "TLSv1.2";

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();
        String JSON_FILE_EXTENSION = "json";

        TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

        Producer producer = new Producer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"));
        producer.connect(tlsConf);

        // Registering extra module is mandatory to handle date time
        ObjectMapper jsonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        String routingKey = getRouting(args);
        String messageString = Files.readString(Path.of(args[1]));
        boolean isJsonScheme = args[1].endsWith(JSON_FILE_EXTENSION);

        if (isJsonScheme) {
            EdxlMessage edxlMessage = jsonMapper.readValue(messageString, EdxlMessage.class);
            producer.publish(routingKey, edxlMessage);
        } else {
            EdxlMessage edxlMessage = xmlMapper.readValue(messageString, EdxlMessage.class);
            producer.xmlPublish(routingKey, edxlMessage);
        }
        producer.close();
    }
}
