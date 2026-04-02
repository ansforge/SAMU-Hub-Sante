package tnr;

import static org.junit.jupiter.api.Assertions.*;
import static tnr.DistributionAssertions.*;

import java.util.Set;
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
    protected static final String V3_TAG = "3.4.0-rc.3";
    protected static final String RS_EDA_REF = "RS-EDA/RS-EDA_partageDossier_DidierMorel.01a.json";
    protected static final String RC_EDA_REF = "RC-EDA/RC-EDA-DouleurThoracique-PierreLegrand.json";
    protected static final String RC_RI_REF           = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.02.json"; // first reception
    protected static final String RC_RI_STATUS1_REF    = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.03.json"; // resource 1 status update
    protected static final String RC_RI_ADD_RES2_REF   = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.05.json"; // add resource 2
    protected static final String RC_RI_STATUS2_REF    = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.06.json"; // resource 1 status update
    protected static final String RC_RI_ADD_RES3_REF   = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.07.json"; // add resource 3 + resource 2 status
    protected static final String RC_RI_ALL_STATUS_REF = "RC-RI/RC-RI_Incendie_RaymondeLECCIA.08.json"; // all statuses updated
    protected static final String RC_RI_RESOURCE_ID  = "fr.fire.sisXXX.cga-XXX.resource.VSR268";
    protected static final String RC_RI_RESOURCE2_ID = "fr.fire.sisXXX.cga-XXX.resource.VSAV1";
    protected static final String RC_RI_RESOURCE3_ID = "fr.fire.sisXXX.cga-XXX.resource.VSAV2";
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
    @DisplayName("RC-RI Raymonde LECCIA lifecycle: resource additions, status updates, and no-op (steps 2, 3, 5–9)")
    void messageRcRiRaymondeLecciaLifecycle() throws Exception {

        String uniqueCaseId = "fr.fire.tnr.test." + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Step 2: first reception — RS-RI + RS-SR
        String step2DistId = sendRcRi(RC_RI_REF, uniqueCaseId);
        assertRsRi(step2DistId);
        assertRsSr(1, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));
        sendAndAssertAck(step2DistId);

        // Step 3: resource 1 status update — RS-SR only
        String step3DistId = sendRcRi(RC_RI_STATUS1_REF, uniqueCaseId);
        assertRsSr(1, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));
        sendAndAssertAck(step3DistId);

        // Step 5: add resource 2 — RS-RI + RS-SR for new resource only
        String step5DistId = sendRcRi(RC_RI_ADD_RES2_REF, uniqueCaseId);
        assertRsRi(step5DistId);
        assertRsSr(1, uniqueCaseId, Set.of(RC_RI_RESOURCE2_ID));
        sendAndAssertAck(step5DistId);

        // Step 6: resource 1 status update — RS-SR only
        String step6DistId = sendRcRi(RC_RI_STATUS2_REF, uniqueCaseId);
        assertRsSr(1, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));
        sendAndAssertAck(step6DistId);

        // Step 7: add resource 3 + resource 2 status update — RS-RI + 2×RS-SR
        String step7DistId = sendRcRi(RC_RI_ADD_RES3_REF, uniqueCaseId);
        assertRsRi(step7DistId);
        assertRsSr(2, uniqueCaseId, Set.of(RC_RI_RESOURCE2_ID, RC_RI_RESOURCE3_ID));
        sendAndAssertAck(step7DistId);

        // Step 8: all resources status update — 3×RS-SR
        String step8DistId = sendRcRi(RC_RI_ALL_STATUS_REF, uniqueCaseId);
        assertRsSr(3, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID, RC_RI_RESOURCE2_ID, RC_RI_RESOURCE3_ID));
        sendAndAssertAck(step8DistId);

        // Step 9: no change — no output
        sendRcRi(RC_RI_ALL_STATUS_REF, uniqueCaseId);
        assertNoMessageReceived();
    }

    private String sendRcRi(String fixtureRef, String caseId) throws Exception {
        String useCase = getUseCaseContentOnline(V3_TAG, fixtureRef)
                .replaceFirst("\"caseId\"\\s*:\\s*\"[^\"]+\"", "\"caseId\": \"" + caseId + "\"");
        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        sendMessage(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY,
                new MessageBuilder().buildMessage(useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V3_ID));
        return distributionId;
    }

    private void assertRsRi(String distributionId) throws Exception {
        MessageDTO rsRi = awaitMessage(distributionId);
        assertNotNull(rsRi, "RS-RI " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(rsRi, VHOST_15_15_V3_TAG);
        assertQueueEquals(rsRi, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(rsRi, "resourcesInfo"), "Expected message type 'resourcesInfo'");
    }

    private void assertRsSr(int count, String expectedCaseId, Set<String> expectedResourceIds) throws Exception {
        java.util.Set<String> received = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            MessageDTO rsSr = awaitMessageOfType("resourcesStatus");
            assertNotNull(rsSr, "RS-SR not received within " + RECEIVE_TIMEOUT_SECS + "s");
            assertVhostEquals(rsSr, VHOST_15_15_V3_TAG);
            assertQueueEquals(rsSr, SAMU1_V3_ID + ".message");
            assertTrue(Utils.isMessageOfType(rsSr, "resourcesStatus"), "Expected message type 'resourcesStatus'");
            com.fasterxml.jackson.databind.JsonNode rsNode = rsSr.getPayload()
                    .path("content").path(0)
                    .path("jsonContent").path("embeddedJsonContent").path("message")
                    .path("resourcesStatus");
            assertEquals(expectedCaseId, rsNode.path("caseId").asText(), "RS-SR caseId mismatch");
            assertFalse(rsNode.has("position"), "RS-SR should not contain a 'position' field (transcoding suppression)");
            received.add(rsNode.path("resourceId").asText());
        }
        assertEquals(expectedResourceIds, received, "RS-SR resource IDs mismatch");
    }

    private void sendAndAssertAck(String referencedDistributionId) throws Exception {
        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID, referencedDistributionId);
        MessageDTO matchedAck = awaitMessage(ackDistributionId);
        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(referencedDistributionId, Utils.getReferencedDistributionID(matchedAck));
    }

}
