package com.hubsante;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

public class CisuMessage {

    @JsonProperty("to")
    public String to;
    @JsonProperty("senderId")
    public String senderId;
    @JsonProperty("distributionId")
    public String distributionId;
    @JsonProperty("content")
    public String content;

    public CisuMessage() {}
    public CisuMessage(String to, String senderId, String distributionId, String content) {
        this.to = to;
        this.senderId = senderId;
        this.distributionId = distributionId;
        this.content = content;
    }

    public String toJsonString() throws IOException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(this);
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getDistributionId() {
        return distributionId;
    }

    public void setDistributionId(String distributionId) {
        this.distributionId = distributionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
