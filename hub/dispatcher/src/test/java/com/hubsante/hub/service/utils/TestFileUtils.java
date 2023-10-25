package com.hubsante.hub.service.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestFileUtils {
    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    public static String getMessageString(boolean isValid, String schemaName, boolean isXML) throws IOException {
        String resourcePath = getTestFilePath(isValid, schemaName, isXML);
        File file = new File(classLoader.getResource(resourcePath).getFile());
        return Files.readString(file.toPath());
    }

    public static String getTestFilePath(boolean isValid, String schemaName, boolean isXML) {
        String subFolder = isValid ? "valid" : "failing";
        String fileName = isValid ? schemaName : schemaName + "-missing-required-fields";
        String extension = isXML ? ".xml" : ".json";

        return "messages/" + subFolder + "/" + schemaName + "/" + fileName + extension;
    }
}
