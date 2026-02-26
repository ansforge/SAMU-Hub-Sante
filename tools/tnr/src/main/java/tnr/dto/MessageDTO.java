package tnr.dto;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Delivery;

public class MessageDTO {

    ObjectMapper mapper = new ObjectMapper();

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

    private ObjectNode parsePayload(Delivery original) throws JsonMappingException, JsonProcessingException {
        String content = new String(original.getBody(), StandardCharsets.UTF_8);
        return mapper.readValue(content, ObjectNode.class);
    }

}
