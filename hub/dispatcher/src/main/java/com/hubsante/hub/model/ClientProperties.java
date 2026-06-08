package com.hubsante.hub.model;

import java.util.List;

public record ClientProperties(
        String clientId,
        boolean useXml,         
        boolean directCisu, String editor,
        List<PerimeterDefinition> perimeters,
        List<String> inhibitedUseCases){
}
