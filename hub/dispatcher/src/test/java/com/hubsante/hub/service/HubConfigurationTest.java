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

    @Test
    public void testFormattedVersion(){
        String input = "1.5,2.0,3.5";
        String[] expectedOutput =  new String[]{"v1","v2", "v3"};
        assertArrayEquals(expectedOutput, HubConfiguration.formatLrmPerimeterVersions(input));

        input = "1.5,";
        expectedOutput =  new String[]{"v1"};
        assertArrayEquals(expectedOutput, HubConfiguration.formatLrmPerimeterVersions(input));

        input = "2.0";
        expectedOutput =  new String[]{"v2"};
        assertArrayEquals(expectedOutput, HubConfiguration.formatLrmPerimeterVersions(input));

        input = "3.2";
        expectedOutput =  new String[]{"v3"};
        assertArrayEquals(expectedOutput, HubConfiguration.formatLrmPerimeterVersions(input));

        Assertions.assertNull(HubConfiguration.formatLrmPerimeterVersions(null));

        input = "4.1";
        expectedOutput =  new String[]{"v4"};
        assertArrayEquals(expectedOutput, HubConfiguration.formatLrmPerimeterVersions(input));
    }
}
