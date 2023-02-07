package com.hubsante;

import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws IOException {
        Path xmlFilePath = Path.of("/Users/romainfouilland/code/ans/docker-rabbitmq-example/schemas/NexSIS/cisu-message.xml");
        String xml = Files.readString(xmlFilePath);
        // public static String xml = "<?xml version=\"1.0\" ?><root><test       attribute=\"text1\">javatpoint</test><test attribute=\"text2\">JTP</test></root>";

        // TODO Auto-generated method stub
        try {
            JSONObject json = XML.toJSONObject(xml);
            String jsonString = json.toString(4);
            System.out.println(jsonString);

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.toString());
        }
    }
}
