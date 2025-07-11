package com.hubsante.hub.utils;

import com.hubsante.hub.service.MessageHandler;
import com.hubsante.model.edxl.EdxlMessage;

import static com.hubsante.hub.config.Constants.CONVERSION_VHOST_MODEL;

public class ConversionRulesCommand {
    MessageHandler messageHandler;
    EdxlMessage edxlMessage;
    String sourceVHost;
    String targetVHost;
    String sourceModelVersion;
    String targetModelVersion;
    Boolean isCisuConversion;


    public ConversionRulesCommand(EdxlMessage edxlMessage, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.edxlMessage = edxlMessage;
        this.sourceVHost = messageHandler.getHubConfig().getVhost();

        String[] targetVhosts = ConversionUtils.getTargetVHosts(messageHandler.getHubConfig(), edxlMessage);
        this.targetVHost = targetVhosts[targetVhosts.length-1];

        this.isCisuConversion = ConversionUtils.requiresCisuConversion(messageHandler.getHubConfig(), edxlMessage);
        this.sourceModelVersion = getVHostMatchingModelVersion(sourceVHost);
        this.targetModelVersion = getVHostMatchingModelVersion(targetVHost);
    }

    public String getTargetVHost() {
        return targetVHost;
    }

    public Boolean getCisuConversion() {
        return isCisuConversion;
    }

    public String getSourceVHost() {
        return sourceVHost;
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

    public String getVHostMatchingModelVersion(String vHost){
        if (CONVERSION_VHOST_MODEL.get(vHost) == null) {
            throw new IllegalArgumentException("There is no model version associated with the host " + vHost);
        }
        return CONVERSION_VHOST_MODEL.get(vHost);
    }
}
