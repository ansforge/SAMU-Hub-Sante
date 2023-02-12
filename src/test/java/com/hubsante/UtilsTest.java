package com.hubsante;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
    private static String[] argv = {"routing.key", "message", "content"};

    @Test
    public void getRouting() {
        assertEquals(Utils.getRouting(argv), "routing.key");
    }

    @Test
    public void getMessage() {
        assertEquals(Utils.getMessage(argv), "message content");
    }
}