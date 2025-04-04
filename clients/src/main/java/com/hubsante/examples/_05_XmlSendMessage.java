package com.hubsante.examples;

import com.hubsante.Producer;
import com.hubsante.TLSConf;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.exception.ValidationException;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.hubsante.Constants.*;
import static com.hubsante.Utils.getRouting;

public class _05_XmlSendMessage {
    private static final Logger logger = LoggerFactory.getLogger(_05_XmlSendMessage.class);

    public static void main(String[] args) throws Exception {
        final Dotenv dotenv = Dotenv.load();

        final TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

        // STEP 1 - Instantiate Producer using environment variables
        final Producer producer = new Producer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"));

        // STEP 2 - Instantiate a EDXL handler and Validator to use Hub utils
        final EdxlHandler edxlHandler = new EdxlHandler();
        final Validator validator = new Validator();

        // STEP 3 - Connect to Hub
        producer.connect(tlsConf);

        // STEP 4 - Get your message. For demo purpose, we pass it as argument in command line
        final String routingKey = getRouting(args);
        String messageFilePath = args[1];
        String stringMessage = Files.readString(Path.of(messageFilePath));

        boolean hasXmlExtension = messageFilePath.endsWith(XML_FILE_EXTENSION);
        if (!hasXmlExtension) {
            logger.warn("You are trying to send a file with the wrong format");
        }

        // STEP 5 - Validate XML message nomenclature
        try {
            validator.validateXML(stringMessage, XML_VALIDATION_SCHEMA);
        } catch(ValidationException error){
            logger.error("The message does not match the validation rules", error);
        } catch (IOException error){
            logger.error("An error occurred while validating the message", error);
        }

        // STEP 6 - Convert message to EDXL standards & publish it
        EdxlMessage edxlMessage = edxlHandler.deserializeXmlEDXL(stringMessage);
        try {
            producer.xmlPublish(routingKey, edxlMessage);
        } catch(IOException error){
            logger.error("An error occurred while sending the message:", error);
        }

        // STEP 7 - Close the connection
        producer.close();
    }
}
