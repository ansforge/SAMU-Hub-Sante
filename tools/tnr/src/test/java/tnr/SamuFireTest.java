package tnr;

import static org.junit.jupiter.api.Assertions.*;
import static tnr.DistributionAssertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tnr.dto.MessageDTO;

class SamuFireTest extends AMQPTestSupport {

    protected static final String SAMU1_V1_ID = "fr.health.tnr.samu1-v1";
    protected static final String SAMU1_V3_ID = "fr.health.tnr.samu1-v3";
    protected static final String SAMU2_V3_ID = "fr.health.tnr.samu2-v3";
    protected static final String VHOST_15_15_V1_TAG = "15-15_v1.5";
    protected static final String VHOST_15_15_V3_TAG = "15-15_v2.1";
    protected static final String V1_TAG = "1.3.0";
    protected static final String V3_TAG = "3.3.0";
    protected static final String RS_EDA_REF = "RS-EDA/RS-EDA_partageDossier_DidierMorel.01a.json";
    protected static final String RC_EDA_REF = "RC-EDA/RC-EDA-DouleurThoracique-PierreLegrand.json";
    protected static final String RC_RI_REF = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.02.json";
    protected static final String RC_RI_RESOURCE_ID = "fr.fire.sisXXX.cga-XXX.resource.VSR268";
    // specific for nexsis
    protected static final String TNR_SDIS_CLIENT_ID = "fr.fire.tnr.sdisZ";
    protected static final String HUB_NEXSIS_USER_CLIENT_ID = "fr.health.fire";
    protected static final String NEXSIS_SHOVEL_ROUTING_KEY = "fr.fire.sga";
    protected static final String VHOST_15_NEXSIS_V3_TAG = "15-nexsis_v1.9";
    protected static final String VHOST_15_NEXSIS_VACTIVE_TAG = "15-nexsis_vactive";


    @Test
    @DisplayName("Send RS-EDA message from samu1_v1 to sdisZ with conversion & transcoding, then send ack")
    void messageFromSamu1V1ToNexsis() throws Exception {

        String useCase = getUseCaseContentOnline(V1_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V1_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, TNR_SDIS_CLIENT_ID);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_VACTIVE_TAG);
        assertQueueEquals(matched, HUB_NEXSIS_USER_CLIENT_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCase"));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_VACTIVE_TAG, NEXSIS_SHOVEL_ROUTING_KEY, SAMU1_V1_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V1_TAG);
        assertQueueEquals(matchedAck, SAMU1_V1_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RS-EDA message from samu1_v3 to sdisZ with transcoding, then send ack")
    void messageFromSamu1V3ToNexsis() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V3_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID);

        sendMessage(VHOST_15_15_V3_TAG, SAMU1_V3_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_VACTIVE_TAG);
        assertQueueEquals(matched, HUB_NEXSIS_USER_CLIENT_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCase"));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_VACTIVE_TAG, NEXSIS_SHOVEL_ROUTING_KEY, SAMU1_V3_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V3_TAG);
        assertQueueEquals(matchedAck, SAMU1_V3_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from samu2_v3 to sdisZ without conversion nor transcoding, then send ack")
    void messageFromSamu2V3ToNexsis() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU2_V3_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU2_V3_ID, TNR_SDIS_CLIENT_ID);

        sendMessage(VHOST_15_NEXSIS_V3_TAG, SAMU2_V3_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_VACTIVE_TAG);
        assertQueueEquals(matched, HUB_NEXSIS_USER_CLIENT_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCase"));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_VACTIVE_TAG, NEXSIS_SHOVEL_ROUTING_KEY, SAMU2_V3_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, SAMU2_V3_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from sdisZ to samu1_v3 with transcoding, then send ack")
    void messageFromNexsisToSamu1V3() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V3_ID);

        sendMessage(VHOST_15_NEXSIS_VACTIVE_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V3_TAG);
        assertQueueEquals(matched, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_VACTIVE_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from sdisZ to samu1_v1 with conversion & transcoding, then send ack")
    void messageFromNexsisToSamu1V1() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V1_ID);

        sendMessage(VHOST_15_NEXSIS_VACTIVE_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V1_TAG);
        assertQueueEquals(matched, SAMU1_V1_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V1_TAG, SAMU1_V1_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_VACTIVE_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from sdisZ to samu2_v3 without conversion nor transcoding, then send ack")
    void messageFromNexsisToSamu2V3() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU2_V3_ID);

        sendMessage(VHOST_15_NEXSIS_VACTIVE_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matched, SAMU2_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCase"));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_V3_TAG, SAMU2_V3_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_VACTIVE_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-RI message (add resource 1 - Raymonde LECCIA) from sdisZ to samu1_v3 with conversion, then check RS-RI (no position) + RS-SR + ack")
    void messageRcRiFromSdisZToSamu1V3WithConversion() throws Exception {

        String useCaseRaw = getUseCaseContentOnline(V3_TAG, RC_RI_REF);
        // Inject a unique caseId to avoid converter deduplication (MongoDB stores by caseId).
        String uniqueCaseId = "fr.fire.tnr.test." + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String useCase = useCaseRaw.replaceFirst(
                "\"caseId\"\\s*:\\s*\"[^\"]+\"",
                "\"caseId\": \"" + uniqueCaseId + "\"");

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V3_ID);

        sendMessage(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        // RS-RI: same distributionId as the source RC-RI, type resourcesInfo, no position field
        MessageDTO matchedRsRi = awaitMessage(distributionId);

        assertNotNull(matchedRsRi, "RS-RI " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedRsRi, VHOST_15_15_V3_TAG);
        assertQueueEquals(matchedRsRi, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matchedRsRi, "resourcesInfo"), "Expected message type 'resourcesInfo'");

        // RS-SR: one per resource in the RC-RI; distributionId generated by the converter (unknown in advance)
        MessageDTO matchedRsSr = awaitMessageOfType("resourcesStatus");

        assertNotNull(matchedRsSr, "RS-SR not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedRsSr, VHOST_15_15_V3_TAG);
        assertQueueEquals(matchedRsSr, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matchedRsSr, "resourcesStatus"), "Expected message type 'resourcesStatus'");
        assertEquals(uniqueCaseId,
                matchedRsSr.getPayload()
                        .path("content").path(0)
                        .path("jsonContent").path("embeddedJsonContent").path("message")
                        .path("resourcesStatus").path("caseId").asText(),
                "RS-SR caseId should match the RC-RI caseId");
        assertEquals(RC_RI_RESOURCE_ID,
                matchedRsSr.getPayload()
                        .path("content").path(0)
                        .path("jsonContent").path("embeddedJsonContent").path("message")
                        .path("resourcesStatus").path("resourceId").asText(),
                "RS-SR resourceId should match the resource from the original RC-RI");

        // Ack sent by SAMU1 v3 back to sdisZ
        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessage(ackDistributionId);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, Utils.getReferencedDistributionID(matchedAck));
    }

}
