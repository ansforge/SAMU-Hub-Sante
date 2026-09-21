package tnr;

public enum MessageType {
    CREATE_CASE("createCase"),
    CREATE_CASE_HEALTH("createCaseHealth"),
    RESOURCES_INFO("resourcesInfo"),
    RESOURCES_INFO_CISU("resourcesInfoCisu"),
    RESOURCES_STATUS("resourcesStatus");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
