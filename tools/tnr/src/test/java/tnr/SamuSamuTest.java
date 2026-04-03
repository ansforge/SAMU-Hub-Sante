package tnr;

import static org.junit.jupiter.api.Assertions.*;
import static tnr.DistributionAssertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import tnr.dto.MessageDTO;

class SamuSamuTest extends AMQPTestSupport {

    protected static final String SAMU1_V1_ID = "fr.health.tnr.samu1-v1";
    protected static final String SAMU2_V1_ID = "fr.health.tnr.samu2-v1";
    protected static final String SAMU1_V2_ID = "fr.health.tnr.samu1-v2";
    protected static final String SAMU2_V2_ID = "fr.health.tnr.samu2-v2";
    protected static final String SAMU1_V3_ID = "fr.health.tnr.samu1-v3";
    protected static final String SAMU2_V3_ID = "fr.health.tnr.samu2-v3";
    protected static final String VHOST_15_15_V1_TAG = "15-15_v1.5";
    protected static final String VHOST_15_15_V2_TAG = "15-15_v2.0";
    protected static final String VHOST_15_15_V3_TAG = "15-15_v2.1";
    protected static final String V3_TAG = "3.3.0";
    protected static final String V2_TAG = "2.3.0";
    protected static final String V1_TAG = "1.3.0";
    protected static final String RS_EDA_REF = "RS-EDA/RS-EDA_partageDossier_DidierMorel.01a.json";

    @Test
    @DisplayName("Send RS-EDA message from samu1_v3 to samu2_v3 without conversion, then send ack")
    void messageFromSamu1V3ToSamu2V3() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V3_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V3_ID, SAMU2_V3_ID);

        sendMessage(VHOST_15_15_V3_TAG, SAMU1_V3_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V3_TAG);
        assertQueueEquals(matched, SAMU2_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU2_V3_ID, SAMU1_V3_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V3_TAG);
        assertQueueEquals(matchedAck, SAMU1_V3_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RS-EDA message from samu1_v2 to samu2_v2 without conversion, then send ack")
    void messageFromSamu1V2ToSamu2V2() throws Exception {

        String useCase = getUseCaseContentOnline(V2_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V2_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V2_ID, SAMU2_V2_ID);

        sendMessage(VHOST_15_15_V2_TAG, SAMU1_V2_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V2_TAG);
        assertQueueEquals(matched, SAMU2_V2_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V2_TAG, SAMU2_V2_ID, SAMU1_V2_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V2_TAG);
        assertQueueEquals(matchedAck, SAMU1_V2_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RS-EDA message from samu1_v1 to samu2_v1 without conversion, then send ack")
    void messageFromSamu1V1ToSamu2V1() throws Exception {

        String useCase = getUseCaseContentOnline(V1_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V1_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, SAMU2_V1_ID);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V1_TAG);
        assertQueueEquals(matched, SAMU2_V1_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V1_TAG, SAMU2_V1_ID, SAMU1_V1_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V1_TAG);
        assertQueueEquals(matchedAck, SAMU1_V1_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RS-EDA message from samu1_v1 to samu1_v3 with conversion, then send ack")
    void messageFromSamu1V1ToSamu1V3() throws Exception {

        String useCase = getUseCaseContentOnline(V1_TAG,  RS_EDA_REF);


        String distributionId = Utils.generateDistributionId(SAMU1_V1_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, SAMU1_V3_ID);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V3_TAG);
        assertQueueEquals(matched, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU1_V3_ID, SAMU1_V1_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V1_TAG);
        assertQueueEquals(matchedAck, SAMU1_V1_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

    @Test
    @DisplayName("Send RS-EDA message from samu1_v3 to samu1_v1 with conversion, then send ack")
    void messageFromSamu1V3ToSamu1V1() throws Exception {

        String useCase = getUseCaseContentOnline(V3_TAG,  RS_EDA_REF);

        String distributionId = Utils.generateDistributionId(SAMU1_V3_ID);
        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V3_ID, SAMU1_V1_ID);

        sendMessage(VHOST_15_15_V3_TAG, SAMU1_V3_ID, edxlJson);

        MessageDTO matched = awaitMessageByDistributionId(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matched, VHOST_15_15_V1_TAG);
        assertQueueEquals(matched, SAMU1_V1_ID + ".message");
        assertTrue(Utils.isMessageOfType(matched, "createCaseHealth"));

        String ackDistributionId = sendAck(VHOST_15_15_V1_TAG, SAMU1_V1_ID, SAMU1_V3_ID, distributionId);

        MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);

        String referencedDistributionID = Utils.getReferencedDistributionID(matchedAck);

        assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(matchedAck, VHOST_15_15_V3_TAG);
        assertQueueEquals(matchedAck, SAMU1_V3_ID + ".ack");
        assertEquals(distributionId, referencedDistributionID);
    }

}
