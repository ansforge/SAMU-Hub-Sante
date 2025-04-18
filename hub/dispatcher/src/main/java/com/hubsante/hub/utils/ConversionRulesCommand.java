package com.hubsante.hub.utils;

import com.hubsante.hub.service.MessageHandler;
import com.hubsante.model.edxl.EdxlMessage;

public class ConversionRulesCommand {
    MessageHandler messageHandler;
    EdxlMessage edxlMessage;
    String sourceVersion;
    String targetVersion;
    String sourceModelVersion;
    String targetModelVersion;
    Boolean isCisuConversion;


    public ConversionRulesCommand(EdxlMessage edxlMessage, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.edxlMessage = edxlMessage;
        this.sourceVersion = ConversionUtils.getSourceVersion(messageHandler.getHubConfig());
        this.targetVersion = ConversionUtils.getTargetVersions(messageHandler.getHubConfig(), edxlMessage)[0]; // todo - choix arbitraire à revoir
        this.isCisuConversion = ConversionUtils.requiresCisuConversion(messageHandler.getHubConfig(), edxlMessage);
        this.sourceModelVersion = getMatchingModelVersion(sourceVersion);
        this.targetModelVersion = getMatchingModelVersion(targetVersion);
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public Boolean getCisuConversion() {
        return isCisuConversion;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public String getSourceModelVersion() {
        return sourceModelVersion;
    }

    public String getTargetModelVersion() {
        return targetModelVersion;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public EdxlMessage getEdxlMessage() {
        return edxlMessage;
    }

    public String getMatchingModelVersion(String perimeterVersion){
        // todo - temp implementation (cf requiresVersionConversion commentary)
        int dotIndex = perimeterVersion.indexOf(".");   // ex: perimeterVersion: 1.5
        if (dotIndex != -1) {
            return "v" + perimeterVersion.substring(0, dotIndex); // ex: "v1"
        }
        return perimeterVersion;
    }
}
