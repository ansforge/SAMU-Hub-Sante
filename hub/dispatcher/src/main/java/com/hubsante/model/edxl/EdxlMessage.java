package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class EdxlMessage extends EdxlEnvelope {

    @JsonProperty(value = "content", required = true)
    private Content content;

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
        if (!super.equals(o)) return false;
        EdxlMessage that = (EdxlMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content);
    }
}
