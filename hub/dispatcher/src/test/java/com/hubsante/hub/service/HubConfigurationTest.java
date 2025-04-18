package com.hubsante.hub.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.hubsante.hub.config.HubConfiguration;

public class HubConfigurationTest {
    @Test
    public void testSplitString() {
        String input = "apple,banana,orange";
        String[] expectedOutput = {"apple", "banana", "orange"};
        assertArrayEquals(expectedOutput, HubConfiguration.splitString(input));

        input = "apple";
        expectedOutput = new String[]{"apple"};
        assertArrayEquals(expectedOutput, HubConfiguration.splitString(input));

        input = "";
        Assertions.assertNull(HubConfiguration.splitString(input));

        input = null;
        Assertions.assertNull(HubConfiguration.splitString(input));

        // todo - enlever le cas des quotes vides
        input = ",apple,,banana,,orange,";
        expectedOutput = new String[]{"","apple", "", "banana", "", "orange"};
        assertArrayEquals(expectedOutput, HubConfiguration.splitString(input));
    }
}
