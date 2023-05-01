package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class ContentXML {

    // TODO bbo : String or JsonNode or equivalent ?
    @JsonProperty(value = "embeddedXMLContent")
    private JsonNode embeddedXMLContent;

    public JsonNode getEmbeddedXMLContent() {
        return embeddedXMLContent;
    }

    public void setEmbeddedXMLContent(JsonNode embeddedXMLContent) {
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
