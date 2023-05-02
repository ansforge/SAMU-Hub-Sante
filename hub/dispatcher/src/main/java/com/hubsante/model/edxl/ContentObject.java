package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Objects;

public class ContentObject {

    @JacksonXmlProperty(localName = "xlink:type", isAttribute = true)
    @JsonIgnore
    public String getXmlns() {
        return "resource";
    }

    @JsonProperty(value = "contentXML")
    private ContentXML contentXML;

    public ContentXML getContentXML() {
        return contentXML;
    }

    public void setContentXML(ContentXML contentXML) {
        this.contentXML = contentXML;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentObject that = (ContentObject) o;
        return Objects.equals(contentXML, that.contentXML);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentXML);
    }

    @Override
    public String toString() {
        return "ContentObject{" +
                "contentXML=" + contentXML +
                '}';
    }
}
