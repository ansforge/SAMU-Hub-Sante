package com.hubsante.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.hubsante.message.CreateEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBootConfiguration
public class CisuCreateEventMessageTest {

    @Test
    public void renderCisuXML() throws IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        File cisuJsonFile = new File(Thread.currentThread().getContextClassLoader().getResource("createEventMessage.json").getFile());
        CreateEventMessage createEventMessage = mapper.readValue(cisuJsonFile, CreateEventMessage.class);

        TemplateLoader loader = new ClassPathTemplateLoader("", ".handlebars");
        Handlebars handlebars = new Handlebars(loader);
        handlebars.setPrettyPrint(true);
        Template template = handlebars.compile("cisu-create-message");

        String xml = template.apply(createEventMessage);
        System.out.println(xml);
        assertDoesNotThrow(() -> {
            Validator validator = initValidator("xsd/cisu.xsd");
            validator.validate(new StreamSource(new StringReader(xml)));
        });

        XmlMapper xmlMapper = (XmlMapper) new XmlMapper().registerModule(new JavaTimeModule());
//        CreateEventMessage message = xmlMapper.readValue(xml, CreateEventMessage.class);
//        assertEquals(createEventMessage, message);
    }

    private Validator initValidator(String xsdPath) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(new File(getClass().getClassLoader().getResource(xsdPath).getFile()));
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }
}
