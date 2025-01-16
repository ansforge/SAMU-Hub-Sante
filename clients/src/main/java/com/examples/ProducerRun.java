package com.examples;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.Producer;
import com.hubsante.TLSConf;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import io.github.cdimascio.dotenv.Dotenv;

import static com.hubsante.Constants.JSON_FILE_EXTENSION;
import static com.hubsante.Constants.TLS_PROTOCOL_VERSION;
import static com.hubsante.Utils.getRouting;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProducerRun {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();
        EdxlHandler edxlHandler = new EdxlHandler();

        TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

        Producer producer = new Producer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"));

        // STEP 1 - Connect to Hub
        producer.connect(tlsConf);

        // STEP 2 - Build message & apply business rules
        String routingKey = getRouting(args);
        String messageFilePath = args[1];
        String stringMessage = Files.readString(Path.of(messageFilePath));

        // STEP 3 - Convert message to EDXL & publish it
        boolean isJsonScheme = messageFilePath.endsWith(JSON_FILE_EXTENSION);
        if (isJsonScheme) {
            EdxlMessage edxlMessage = edxlHandler.deserializeJsonEDXL(stringMessage);
            producer.publish(routingKey, edxlMessage);
        } else {
            EdxlMessage edxlMessage = edxlHandler.deserializeXmlEDXL(stringMessage);
            producer.xmlPublish(routingKey, edxlMessage);
        }

        // STEP 4 - Close the connection
        producer.close();
    }
}
