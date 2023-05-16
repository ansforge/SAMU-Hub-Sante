package com.hubsante.model.builders;

import com.hubsante.model.cisu.*;

import java.time.OffsetDateTime;

public class AckMessageBuilder {
    private String messageId;
    private AddresseeType sender;
    private java.time.OffsetDateTime sentAt;
    private MsgType msgType;
    private Status status;
    private Recipients recipients;
    private AckMessageId ackMessage;

    public AckMessageBuilder withMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public AckMessageBuilder withSender(AddresseeType sender) {
        this.sender = sender;
        return this;
    }

    public AckMessageBuilder withSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public AckMessageBuilder withMsgType(MsgType msgType) {
        this.msgType = msgType;
        return this;
    }

    public AckMessageBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }

    public AckMessageBuilder withRecipients(Recipients recipients) {
        this.recipients = recipients;
        return this;
    }

    public AckMessageBuilder withAckMessage(AckMessageId ackMessageId) {
        this.ackMessage = ackMessageId;
        return this;
    }

    public AckMessage build() {
        AckMessage ackMsg = new AckMessage();
        ackMsg.setMessageId(this.messageId);
        ackMsg.setSender(this.sender);
        ackMsg.setSentAt(this.sentAt);
        ackMsg.setMsgType(this.msgType);
        ackMsg.setStatus(this.status);
        ackMsg.setRecipients(this.recipients);
        ackMsg.setAckMessage(this.ackMessage);

        return ackMsg;
    }
}
