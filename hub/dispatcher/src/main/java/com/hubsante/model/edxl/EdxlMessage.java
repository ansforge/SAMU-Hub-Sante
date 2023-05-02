package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.OffsetDateTime;
import java.util.Objects;

@JacksonXmlRootElement(localName = "edxlDistribution")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EdxlMessage {

    @JsonProperty(value = "distributionID", required = true)
    private String distributionID;

    @JsonProperty(value = "senderID", required = true)
    private String senderID;

    @JsonProperty(value = "dateTimeSent", required = true)
    private OffsetDateTime dateTimeSent;

    @JsonProperty(value = "dateTimeExpires", required = true)
    private OffsetDateTime dateTimeExpires;

    @JsonProperty(value = "distributionStatus", required = true)
    private DistributionStatus distributionStatus;

    @JsonProperty(value = "distributionKind", required = true)
    private DistributionKind distributionKind;

    @JsonProperty(value = "descriptor", required = true)
    private Descriptor descriptor;

    @JsonProperty(value = "content", required = true)
    private Content content;

    @JacksonXmlProperty(localName = "xlink:type", isAttribute = true)
    @JsonIgnore
    public String getXmlnsXlinkType() {
        return "extended";
    }

    @JacksonXmlProperty(localName = "xmlns", isAttribute = true)
    @JsonIgnore
    public String getXmlns() {
        return "urn:oasis:names:tc:emergency:EDXL:DE:2.0";
    }

    @JacksonXmlProperty(localName = "xmlns:xlink", isAttribute = true)
    @JsonIgnore
    public String getXmlnsXlink() {
        return "http://www.w3.org/1999/xlink";
    }

    @JacksonXmlProperty(localName = "xmlns:ct", isAttribute = true)
    @JsonIgnore
    public String getXmlnsCt() {
        return "urn:oasis:names:tc:emergency:edxl:ct:1.0";
    }

    public EdxlMessage() {
    }

    public String getDistributionID() {
        return distributionID;
    }

    public void setDistributionID(String distributionID) {
        this.distributionID = distributionID;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public OffsetDateTime getDateTimeSent() {
        return dateTimeSent;
    }

    public void setDateTimeSent(OffsetDateTime dateTimeSent) {
        this.dateTimeSent = dateTimeSent;
    }

    public OffsetDateTime getDateTimeExpires() {
        return dateTimeExpires;
    }

    public void setDateTimeExpires(OffsetDateTime dateTimeExpires) {
        this.dateTimeExpires = dateTimeExpires;
    }

    public DistributionStatus getDistributionStatus() {
        return distributionStatus;
    }

    public void setDistributionStatus(DistributionStatus distributionStatus) {
        this.distributionStatus = distributionStatus;
    }

    public DistributionKind getDistributionKind() {
        return distributionKind;
    }

    public void setDistributionKind(DistributionKind distributionKind) {
        this.distributionKind = distributionKind;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdxlMessage that = (EdxlMessage) o;
        return
                distributionID.equals(that.distributionID) &&
                senderID.equals(that.senderID) &&
                dateTimeSent.equals(that.dateTimeSent) &&
                dateTimeExpires.equals(that.dateTimeExpires) &&
                distributionStatus == that.distributionStatus &&
                distributionKind == that.distributionKind &&
                descriptor.equals(that.descriptor) &&
                content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distributionID, senderID, dateTimeSent, dateTimeExpires, distributionStatus, distributionKind, descriptor, content);
    }

    @Override
    public String toString() {
        return "class EdxlMessage {\n" +
                "    distributionID: " + toIndentedString(distributionID) + "\n" +
                "    senderId: " + toIndentedString(senderID) + "\n" +
                "    dateTimeSent: " + toIndentedString(dateTimeSent) + "\n" +
                "    dateTimeExpires: " + toIndentedString(dateTimeExpires) + "\n" +
                "    distributionStatus: " + toIndentedString(distributionStatus) + "\n" +
                "    distributionKind: " + toIndentedString(distributionKind) + "\n" +
                "    descriptor: " + toIndentedString(descriptor) + "\n" +
                "    content: " + toIndentedString(content) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
