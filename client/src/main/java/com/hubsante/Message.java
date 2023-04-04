package com.hubsante;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

public class Message {
    public String to;
    public String senderId;
    public String distributionId;
    public String content;

    public Message(String to, String senderId, String distributionId, String content) {
        this.to = to;
        this.senderId = senderId;
        this.distributionId = distributionId;
        this.content = content;
    }

    public String toJsonString() throws IOException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(this);
    }
}
