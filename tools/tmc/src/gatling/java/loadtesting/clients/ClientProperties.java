package loadtesting.clients;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClientProperties(
        @JsonProperty("client_id") String clientId,
        List<PerimeterDefinition> perimeters) {}
