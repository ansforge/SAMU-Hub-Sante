package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Objects;

@JsonRootName(value = "JsonContent")
@JacksonXmlRootElement(localName = "ContentXML")
public class ContentXML {

    @JsonProperty(value = "embeddedJSONContent")
    @JacksonXmlProperty(localName = "embeddedXMLContent")
    private ContentWrapper embeddedXMLContent;

    public ContentWrapper getEmbeddedXMLContent() {
        return embeddedXMLContent;
    }

    public void setEmbeddedXMLContent(ContentWrapper embeddedXMLContent) {
        this.embeddedXMLContent = embeddedXMLContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentXML that = (ContentXML) o;
        return Objects.equals(embeddedXMLContent, that.embeddedXMLContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(embeddedXMLContent);
    }

    @Override
    public String toString() {
        return "ContentXML{" +
                "embeddedXMLContent=" + embeddedXMLContent.toString() +
                '}';
    }
}
