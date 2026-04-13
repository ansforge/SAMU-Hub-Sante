package tnr;

import static org.junit.jupiter.api.Assertions.*;
import static tnr.DistributionAssertions.*;
import static tnr.TestConstants.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import tnr.MessageType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tnr.dto.MessageDTO;

class SamuFireTest extends AMQPTestSupport {

    @Test
    @DisplayName("Send RS-EDA message from samu1_v1 to sdisZ with conversion & transcoding, then send ack")
    void messageFromSamu1V1ToNexsis() throws Exception {

        String useCase = getUseCaseContentOnline(V1_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V1_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, TNR_SDIS_CLIENT_ID);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matched, HUB_NEXSIS_USER_CLIENT_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, SAMU1_V1_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V1_TAG);
        assertQueueEquals(matchedAck, SAMU1_V1_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RS-EDA message from samu1_v3 to sdisZ with transcoding, then send ack")
    void messageFromSamu1V3ToNexsis() throws Exception {

        String useCase = getUseCaseContentOnline(V3_FIRE_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V3_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID);

        sendMessage(VHOST_15_15_V3_TAG, SAMU1_V3_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matched, HUB_NEXSIS_USER_CLIENT_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, SAMU1_V3_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V3_TAG);
        assertQueueEquals(matchedAck, SAMU1_V3_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from samu2_v3 to sdisZ without conversion nor transcoding, then send ack")
    void messageFromSamu2V3ToNexsis() throws Exception {

        String useCase = getUseCaseContentOnline(V3_FIRE_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU2_V3_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU2_V3_ID, TNR_SDIS_CLIENT_ID);

        sendMessage(VHOST_15_NEXSIS_V3_TAG, SAMU2_V3_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matched, HUB_NEXSIS_USER_CLIENT_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, SAMU2_V3_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, SAMU2_V3_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from sdisZ to samu1_v3 with transcoding, then send ack")
    void messageFromNexsisToSamu1V3() throws Exception {

        String useCase = getUseCaseContentOnline(V3_FIRE_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V3_ID);

        sendMessage(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V3_TAG);
        assertQueueEquals(matched, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE_HEALTH));

        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from sdisZ to samu1_v1 with conversion & transcoding, then send ack")
    void messageFromNexsisToSamu1V1() throws Exception {

        String useCase = getUseCaseContentOnline(V3_FIRE_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V1_ID);

        sendMessage(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V1_TAG);
        assertQueueEquals(matched, SAMU1_V1_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE_HEALTH));

        String ackDistributionId = sendAck(VHOST_15_15_V1_TAG, SAMU1_V1_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RC-EDA message from sdisZ to samu2_v3 without conversion nor transcoding, then send ack")
    void messageFromNexsisToSamu2V3() throws Exception {

        String useCase = getUseCaseContentOnline(V3_FIRE_TAG,  RC_EDA_REF);

        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU2_V3_ID);

        sendMessage(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matched, SAMU2_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE));

        String ackDistributionId = sendAck(VHOST_15_NEXSIS_V3_TAG, SAMU2_V3_ID, TNR_SDIS_CLIENT_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_NEXSIS_V3_TAG);
        assertQueueEquals(matchedAck, HUB_NEXSIS_USER_CLIENT_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("RC-RI Raymonde LECCIA lifecycle: resource additions, status updates, and no-op (steps 2, 3, 5–9)")
    void messageRcRiRaymondeLecciaLifecycle() throws Exception {

        String uniqueCaseId = "fr.fire.tnr.test." + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Step 2: first reception — RS-RI + RS-SR
        String step2DistId = sendRcRi(RC_RI_REF, uniqueCaseId);
        MessageDTO step2RsRi = awaitMessageByDistributionId(step2DistId);
        List<MessageDTO> step2RsSr = List.of(awaitMessageOfType(MessageType.RESOURCES_STATUS));
        assertRsRi(step2RsRi, step2DistId);
        assertRsSr(step2RsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));
        sendAndAssertAck(step2DistId);

        // Step 3: resource 1 status update — RS-SR only
        String step3DistId = sendRcRi(RC_RI_STATUS1_REF, uniqueCaseId);
        List<MessageDTO> step3RsSr = List.of(awaitMessageOfType(MessageType.RESOURCES_STATUS));
        assertRsSr(step3RsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));
        sendAndAssertAck(step3DistId);

        // Step 5: add resource 2 — RS-RI + RS-SR for new resource only
        String step5DistId = sendRcRi(RC_RI_ADD_RES2_REF, uniqueCaseId);
        MessageDTO step5RsRi = awaitMessageByDistributionId(step5DistId);
        List<MessageDTO> step5RsSr = List.of(awaitMessageOfType(MessageType.RESOURCES_STATUS));
        assertRsRi(step5RsRi, step5DistId);
        assertRsSr(step5RsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE2_ID));
        sendAndAssertAck(step5DistId);

        // Step 6: resource 1 status update — RS-SR only
        String step6DistId = sendRcRi(RC_RI_STATUS2_REF, uniqueCaseId);
        List<MessageDTO> step6RsSr = List.of(awaitMessageOfType(MessageType.RESOURCES_STATUS));
        assertRsSr(step6RsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));
        sendAndAssertAck(step6DistId);

        // Step 7: add resource 3 + resource 2 status update — RS-RI + 2×RS-SR
        String step7DistId = sendRcRi(RC_RI_ADD_RES3_REF, uniqueCaseId);
        MessageDTO step7RsRi = awaitMessageByDistributionId(step7DistId);
        assertRsRi(step7RsRi, step7DistId);
        List<MessageDTO> step7RsSr = List.of(
                awaitMessageOfType(MessageType.RESOURCES_STATUS),
                awaitMessageOfType(MessageType.RESOURCES_STATUS));
        assertRsSr(step7RsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE2_ID, RC_RI_RESOURCE3_ID));
        sendAndAssertAck(step7DistId);

        // Step 8: all resources status update — 3×RS-SR
        String step8DistId = sendRcRi(RC_RI_ALL_STATUS_REF, uniqueCaseId);
        List<MessageDTO> step8RsSr = List.of(
                awaitMessageOfType(MessageType.RESOURCES_STATUS),
                awaitMessageOfType(MessageType.RESOURCES_STATUS),
                awaitMessageOfType(MessageType.RESOURCES_STATUS));
        assertRsSr(step8RsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID, RC_RI_RESOURCE2_ID, RC_RI_RESOURCE3_ID));
        sendAndAssertAck(step8DistId);

        // Step 9: no change — no output
        sendRcRi(RC_RI_ALL_STATUS_REF, uniqueCaseId);
        assertNoMessageReceived(msg -> msg.getQueue().equals(SAMU1_V3_ID + ".message"));
    }

    private String sendRcRi(String fixtureRef, String caseId) throws Exception {
        String useCase = getUseCaseContentOnline(V3_FIRE_TAG, fixtureRef)
                .replaceFirst("\"caseId\"\\s*:\\s*\"[^\"]+\"", "\"caseId\": \"" + caseId + "\"");
        String distributionId = Utils.generateDistributionId(TNR_SDIS_CLIENT_ID);
        sendMessage(VHOST_15_NEXSIS_V3_TAG, NEXSIS_SHOVEL_ROUTING_KEY,
                new MessageBuilder().buildMessage(useCase, distributionId, TNR_SDIS_CLIENT_ID, SAMU1_V3_ID));
        return distributionId;
    }

    private void sendAndAssertAck(String referencedDistributionId) throws Exception {
        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU1_V3_ID, TNR_SDIS_CLIENT_ID, referencedDistributionId);
        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);
        assertAck(matchedAck, ackDistributionId, VHOST_15_NEXSIS_V3_TAG, HUB_NEXSIS_USER_CLIENT_ID + ".ack", referencedDistributionId);
    }

}
