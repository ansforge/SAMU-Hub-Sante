package com.hubsante;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
    private static String[] argv = {"fr.health.hub.samu001.message", "message", "content"};

    @Test
    public void getRouting() {
        assertEquals(Utils.getRouting(argv), "fr.health.hub.samu001.message");
    }

    @Test
    public void getClientId() {
        assertEquals(Utils.getClientId(argv), "fr.health.hub.samu001");
    }
}