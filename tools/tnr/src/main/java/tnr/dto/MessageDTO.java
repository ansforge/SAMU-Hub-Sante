package tnr.dto;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.rabbitmq.client.Delivery;

public class MessageDTO {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

    private final String vhost;
    private final String queue;
    private final ObjectNode payload;
    private final String distributionId;
    private final Delivery original;

    public MessageDTO(String vhost, String queue, Delivery original) throws Exception {
        this.vhost = vhost;
        this.queue = queue;
        this.payload = this.parsePayload(original);
        this.original = original;
        this.distributionId = this.extractDistributionId(payload);
    }

    public String getVhost() {
        return vhost;
    }

    public String getQueue() {
        return queue;
    }

    public ObjectNode getPayload() {
        return payload;
    }

    public String getDistributionId() {
        return distributionId;
    }

    public Delivery getDelivery() {
        return original;
    }

    private String extractDistributionId(ObjectNode payload) throws Exception {
        return payload.get("distributionID").asText();
    }

    private ObjectNode parsePayload(Delivery original) throws Exception {
        String content = new String(original.getBody(), StandardCharsets.UTF_8).trim();

        JsonNode node;
        if (isXML(original)) {
            node = xmlMapper.readTree(content.getBytes(StandardCharsets.UTF_8));
        } else {
            node = jsonMapper.readTree(content);
        }

        if (node instanceof ObjectNode objectNode) {
            return objectNode;
        }

        throw new IllegalArgumentException("Payload is not valid JSON/XML.");
    }

    private static boolean isXML(Delivery original) {
        return (original.getProperties().getContentType().equals("application/xml"));
    }
}
