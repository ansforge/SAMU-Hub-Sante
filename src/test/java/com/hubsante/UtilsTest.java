package com.hubsante;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
    private static String[] argv = {"clientId.in.message", "message", "content"};

    @Test
    public void getRouting() {
        assertEquals(Utils.getRouting(argv), "routing.key");
    }

    @Test
    public void getClientId() {
        assertEquals(Utils.getClientId(argv), "clientId");
    }

    @Test
    public void getMessage() {
        assertEquals(Utils.getMessage(argv), "message content");
    }

    @Test
    public void getMessageType() {
        assertEquals(Utils.getMessageType("clientId.out.message"), "message");
    }
}