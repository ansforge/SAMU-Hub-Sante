package com.hubsante.model.edxl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hubsante.message.CisuMessage;
import com.hubsante.message.CreateEventMessage;
import com.hubsante.model.emsi.Emsi;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ContentWrapper {

    // TODO bbo : replace by CisuMessage with JsonSubType mapping depending on content ?
    private CreateEventMessage message;
    private Emsi emsi;

    public CreateEventMessage getMessage() {
        return message;
    }

    public void setMessage(CreateEventMessage message) {
        this.message = message;
    }

    public Emsi getEmsi() {
        return emsi;
    }

    public void setEmsi(Emsi emsi) {
        this.emsi = emsi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentWrapper that = (ContentWrapper) o;
        return Objects.equals(message, that.message) && Objects.equals(emsi, that.emsi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, emsi);
    }
}
