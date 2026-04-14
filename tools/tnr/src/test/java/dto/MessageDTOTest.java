package dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import tnr.dto.MessageDTO;
import org.junit.jupiter.api.Test;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;


class MessageDTOTest {

    @Test
    void shouldParseJsonPayload() throws Exception {
        String json = """
                {
                   "distributionID": "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea"
                 }
            """;

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .build();
        Envelope envelope = new Envelope(
                1L,     // deliveryTag
                false,  // redeliver
                "exchange",
                "routingKey"
        );
        Delivery delivery = new Delivery(
                envelope,
                props,
                json.getBytes(StandardCharsets.UTF_8)
        );

        MessageDTO dto = new MessageDTO("test-vhost", "test-queue", delivery);

        assertNotNull(dto.getPayload());
        assertEquals("fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea", dto.getDistributionId());
        assertEquals("fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea", dto.getPayload().get("distributionID").asText());
    }

    @Test
    void shouldParseXmlPayloadAsObjectNode() throws Exception {
        String xml = """
            <edxlDistribution xlink:type="extended" xmlns="urn:oasis:names:tc:emergency:EDXL:DE:2.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:ct="urn:oasis:names:tc:emergency:edxl:ct:1.0">
                  <distributionID>fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea</distributionID>
             </edxlDistribution>
            """;

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/xml")
                .build();
        Envelope envelope = new Envelope(
                1L,     // deliveryTag
                false,  // redeliver
                "exchange",
                "routingKey"
        );
        Delivery delivery = new Delivery(
                envelope,
                props,
                xml.getBytes(StandardCharsets.UTF_8)
        );
        MessageDTO dto = new MessageDTO("test-vhost", "test-queue", delivery);

        assertNotNull(dto.getPayload());
        assertEquals("fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea", dto.getDistributionId());
        assertEquals("fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea", dto.getPayload().get("distributionID").asText());
    }

    @Test
    void shouldThrowWhenPayloadIsNotObject() {

        String invalidPayload = """
            [1,2,3]
            """;
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .build();
        Envelope envelope = new Envelope(1L, false, "exchange", "routingKey");
        Delivery delivery = new Delivery(
                envelope,
                props,
                invalidPayload.getBytes(StandardCharsets.UTF_8)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageDTO("test-vhost", "test-queue", delivery)
        );
    }
}