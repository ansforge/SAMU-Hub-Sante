package com.hubsante.hub.model;

import java.util.List;

public record PerimeterDefinition(
        String name,
        List<String> versions) {
}
