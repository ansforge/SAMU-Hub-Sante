package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Content {

    @JsonProperty(value = "contentObject")
    private ContentObject contentObject;

    public ContentObject getContentObject() {
        return contentObject;
    }

    public void setContentObject(ContentObject contentObject) {
        this.contentObject = contentObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content content = (Content) o;
        return Objects.equals(contentObject, content.contentObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentObject);
    }

    @Override
    public String toString() {
        return "Content{" +
                "contentObject=" + contentObject +
                '}';
    }
}
