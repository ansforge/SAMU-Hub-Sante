package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ContentObject {

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
