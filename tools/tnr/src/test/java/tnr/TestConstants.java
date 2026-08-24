package tnr;

public final class TestConstants {

    private TestConstants() {
    }

    public static final int RECEIVE_TIMEOUT_SECS = 10;

    // Client IDs — SAMU
    public static final String SAMU1_V1_ID = "fr.health.tnr.samu1-v1";
    public static final String SAMU2_V1_ID = "fr.health.tnr.samu2-v1";
    public static final String SAMU1_V2_ID = "fr.health.tnr.samu1-v2";
    public static final String SAMU2_V2_ID = "fr.health.tnr.samu2-v2";
    public static final String SAMU1_V3_ID = "fr.health.tnr.samu1-v3";
    public static final String SAMU2_V3_ID = "fr.health.tnr.samu2-v3";

    // Client IDs — Nexsis
    public static final String TNR_SDIS_CLIENT_ID = "fr.fire.tnr.sdisZ";
    public static final String HUB_NEXSIS_USER_CLIENT_ID = "fr.health.fire";
    public static final String NEXSIS_SHOVEL_ROUTING_KEY = "fr.fire.sga";

    // Vhosts
    public static final String VHOST_15_15_V1_TAG = "15-15_v1.5";
    public static final String VHOST_15_15_V2_TAG = "15-15_v2.0";
    public static final String VHOST_15_15_V3_TAG = "15-15_v2.1";
    public static final String VHOST_15_NEXSIS_V3_TAG = "15-nexsis_v1.9";

    // Version tags
    public static final String V1_TAG = "1.3.0";
    public static final String V2_TAG = "2.3.0";
    public static final String V3_SAMU_TAG = "3.3.0";
    public static final String V3_FIRE_TAG = "3.4.0-rc.3";

    // Use case references — shared
    public static final String RS_EDA_REF = "RS-EDA/RS-EDA_partageDossier_DidierMorel.01a.json";

    // Use case references — RC-EDA
    public static final String RC_EDA_REF = "RC-EDA/RC-EDA-DouleurThoracique-PierreLegrand.json";

    // Use case references — RC-RI (Raymonde LECCIA lifecycle)
    public static final String RC_RI_REF           = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.02.json"; // first reception
    public static final String RC_RI_STATUS1_REF    = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.03.json"; // resource 1 status update
    public static final String RC_RI_ADD_RES2_REF   = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.05.json"; // add resource 2
    public static final String RC_RI_STATUS2_REF    = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.06.json"; // resource 1 status update
    public static final String RC_RI_ADD_RES3_REF   = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.07.json"; // add resource 3 + resource 2 status
    public static final String RC_RI_ALL_STATUS_REF = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.08.json"; // all statuses updated

    // Resource IDs — RC-RI
    public static final String RC_RI_RESOURCE_ID  = "fr.fire.sisXXX.cga-XXX.resource.VSR268";
    public static final String RC_RI_RESOURCE2_ID = "fr.fire.sisXXX.cga-XXX.resource.VSAV1";
    public static final String RC_RI_RESOURCE3_ID = "fr.fire.sisXXX.cga-XXX.resource.VSAV2";

    // specific for nexsis
    protected static final String VHOST_15_NEXSIS_VACTIVE_TAG = "15-nexsis_vactive";

    // Use case references — RS-RI/RS-SR (Alice & Grégoire NORMAND FuiteDeGaz lifecycle)
    public static final String RS_RI_NORMAND_REF = "RS-RI/RS-RI_FuiteDeGaz_AliceGregoireNORMAND.03.json"; // SMUR resource with status
    public static final String RS_SR_NORMAND_REF = "RS-SR/RS-SR_FuiteDeGaz_AliceGregoireNORMAND.04.json"; // status update

    // Use case references — RS-RI/RS-SR (Robert VERMANDE lifecycle)
    public static final String RS_RI_VERMANDE_REF = "RS-RI/RS-RI_Secondaire_RobertVermande.03.json"; // SMUR resource without status
    public static final String RS_SR_VERMANDE_REF = "RS-SR/RS-SR_Secondaire_RobertVermande.06.json"; // adds status

    // Use case references — RS-RI (Monsieur X non-SMUR)
    public static final String RS_RI_MONSIEUR_X_REF = "RS-RI/RS-RI_partageRessources_MonsieurX.03.json"; // TSU resource with status

    // Resource IDs — RS-RI
    public static final String RS_RI_NORMAND_RESOURCE_ID  = "fr.health.samu440.resource.VLM2";
    public static final String RS_RI_VERMANDE_RESOURCE_ID = "fr.health.samu680.resource.AR1";
}
