package com.hubsante.model.builders;

import com.hubsante.model.cisu.AckMessage;
import com.hubsante.model.cisu.CreateEventMessage;
import com.hubsante.model.edxl.*;
import com.hubsante.model.emsi.Emsi;

import java.time.OffsetDateTime;

public class EdxlMessageBuilder {

    private String distributionID;
    private String senderID;
    private OffsetDateTime dateTimeSent;
    private OffsetDateTime dateTimeExpires;
    private DistributionStatus distributionStatus;
    private DistributionKind distributionKind;
    private Descriptor descriptor;

    private Content content;

    public EdxlMessageBuilder() { }

    public EdxlMessageBuilder withDistributionID(String distributionID) {
        this.distributionID = distributionID;
        return this;
    }

    public EdxlMessageBuilder withSenderID(String senderID) {
        this.senderID = senderID;
        return this;
    }

    public EdxlMessageBuilder withDateTimeSent(OffsetDateTime dateTimeSent) {
        this.dateTimeSent = dateTimeSent;
        return this;
    }

    public EdxlMessageBuilder withDateTimeExpires(OffsetDateTime dateTimeExpires) {
        this.dateTimeExpires = dateTimeExpires;
        return this;
    }

    public EdxlMessageBuilder withDistributionStatus(DistributionStatus distributionStatus) {
        this.distributionStatus = distributionStatus;
        return this;
    }

    public EdxlMessageBuilder withDistributionKind(DistributionKind distributionKind) {
        this.distributionKind = distributionKind;
        return this;
    }

    public EdxlMessageBuilder withDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
        return this;
    }

    public EdxlMessageBuilder withContent(CreateEventMessage createEventMessage) {
        this.content = new Content(new ContentObject(new ContentWrapper(new EmbeddedContent(createEventMessage))));
        return this;
    }

    public EdxlMessageBuilder withContent(AckMessage ackMessage) {
        this.content = new Content(new ContentObject(new ContentWrapper(new EmbeddedContent(ackMessage))));
        return this;
    }

    public EdxlMessageBuilder withContent(Emsi emsi) {
        this.content = new Content(new ContentObject(new ContentWrapper(new EmbeddedContent(emsi))));
        return this;
    }

    public EdxlMessage build() {
        EdxlMessage edxlMessage = new EdxlMessage();
        edxlMessage.setDistributionID(this.distributionID);
        edxlMessage.setSenderID(this.senderID);
        edxlMessage.setDateTimeSent(this.dateTimeSent);
        edxlMessage.setDateTimeExpires(this.dateTimeExpires);
        edxlMessage.setDistributionKind(this.distributionKind);
        edxlMessage.setDistributionStatus(this.distributionStatus);
        edxlMessage.setDescriptor(this.descriptor);
        edxlMessage.setContent(this.content);

        return edxlMessage;
    }
}
