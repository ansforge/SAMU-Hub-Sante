package com.hubsante.hub.utils;

import com.hubsante.hub.service.MessageHandler;
import com.hubsante.model.edxl.EdxlMessage;

public class ApplyConversionRulesCommand {
    MessageHandler messageHandler;
    EdxlMessage edxlMessage;
    String sourceVersion;
    String targetVersion;
    Boolean isCisuConversion;


    public ApplyConversionRulesCommand(EdxlMessage edxlMessage, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.edxlMessage = edxlMessage;
        this.sourceVersion = ConversionUtils.getSourceVersion(messageHandler.getHubConfig());
        this.targetVersion = ConversionUtils.getTargetVersions(messageHandler.getHubConfig(), edxlMessage)[0]; // todo - choix arbitraire à revoir
        this.isCisuConversion = ConversionUtils.requiresCisuConversion(messageHandler.getHubConfig(), edxlMessage);
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

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public EdxlMessage getEdxlMessage() {
        return edxlMessage;
    }
}
