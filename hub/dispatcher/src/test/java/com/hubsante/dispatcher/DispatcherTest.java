package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.Dispatcher;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
@Slf4j
public class DispatcherTest {

    private RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
    @Autowired
    private EdxlHandler converter;
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Test
    public void test() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.xml").getFile());
        String xml = Files.readString(edxlCisuCreateFile.toPath());

        MessageProperties properties = MessagePropertiesBuilder.newInstance().setReceivedRoutingKey("fr.health.hub.samu110.out.message").build();
        Message message = new Message(xml.getBytes(StandardCharsets.UTF_8), properties);

        Dispatcher dispatcher = new Dispatcher(rabbitTemplate, converter);
        dispatcher.dispatch(message);
        Mockito.verify(rabbitTemplate, times(1)).send(eq(""), eq("fr.fire.nexsis.sdis23.in.message"), any());
    }
}
