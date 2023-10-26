package com.hubsante.model.cisu;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

@JsonPropertyOrder({
        Nomenclature.JSON_PROPERTY_CODE,
        Nomenclature.JSON_PROPERTY_LABEL,
        Nomenclature.JSON_PROPERTY_FREETEXT
})
@JsonTypeName("riskThreat")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RiskThreat {

    public static final String JSON_PROPERTY_CODE = "code";
    private String code;

    public static final String JSON_PROPERTY_LABEL = "label";
    private String label;

    public static final String JSON_PROPERTY_FREETEXT = "freetext";
    private String freetext;

    public RiskThreat() {
    }

    public RiskThreat code(String code) {

        this.code = code;
        return this;
    }

    /**
     * Décrit le code de motif de recours médical
     *
     * @return code
     **/
    @JsonProperty(JSON_PROPERTY_CODE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)

    public String getCode() {
        return code;
    }


    @JsonProperty(JSON_PROPERTY_CODE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setCode(String code) {
        this.code = code;
    }


    public RiskThreat label(String label) {

        this.label = label;
        return this;
    }

    /**
     * Libellé du motif
     *
     * @return label
     **/
    @JsonProperty(JSON_PROPERTY_LABEL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)

    public String getLabel() {
        return label;
    }


    @JsonProperty(JSON_PROPERTY_LABEL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setLabel(String label) {
        this.label = label;
    }


    public RiskThreat freetext(String freetext) {

        this.freetext = freetext;
        return this;
    }

    /**
     * Get freetext
     *
     * @return freetext
     **/
    @JsonProperty(JSON_PROPERTY_FREETEXT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getFreetext() {
        return freetext;
    }


    @JsonProperty(JSON_PROPERTY_FREETEXT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setFreetext(String freetext) {
        this.freetext = freetext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RiskThreat nomenclature = (RiskThreat) o;
        return Objects.equals(this.code, nomenclature.code) &&
                Objects.equals(this.label, nomenclature.label) &&
                Objects.equals(this.freetext, nomenclature.freetext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code
                , label
                , freetext);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RiskThreat {\n");
        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    label: ").append(toIndentedString(label)).append("\n");
        sb.append("    freetext: ").append(toIndentedString(freetext)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
