package com.hubsante.hub.utils;

import com.hubsante.hub.service.MessageHandler;
import com.hubsante.model.edxl.EdxlMessage;

import static com.hubsante.hub.config.Constants.VHOST_MODEL_VERSION;

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
        this.targetVHost = ConversionUtils.getTargetVHosts(messageHandler.getHubConfig(), edxlMessage)[0]; // todo - choix arbitraire à revoir
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
        if (VHOST_MODEL_VERSION.get(vHost) == null) {
            throw new IllegalArgumentException("There is no model version associated with the host " + vHost);
        }
        return VHOST_MODEL_VERSION.get(vHost);
//         todo - temp implementation (cf requiresVersionConversion commentary)
//        int dotIndex = perimeterVersion.indexOf(".");   // ex: perimeterVersion: 1.5
//        if (dotIndex != -1) {
//            return "v" + perimeterVersion.substring(0, dotIndex); // ex: "v1"
//        }
//
//
//        return perimeterVersion;
    }
}
