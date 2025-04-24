package com.hubsante.hub.utils;

import com.hubsante.hub.service.MessageHandler;
import com.hubsante.model.edxl.EdxlMessage;

import java.util.Objects;

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
        // todo - revoir implementation pour utiliser un map commune
        if(Objects.equals(perimeterVersion, "1.5")) return "v1";
        if(Objects.equals(perimeterVersion, "2.0")) return "v2";
        if(Objects.equals(perimeterVersion, "2.1")) return "v3";
        else return "v1";
    }
}
