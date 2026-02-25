package tnr;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.DistributionStatus;

import tnr.dto.MessageDTO;

class AppTest extends AMQPTestSupport {

    protected static final String SAMU1_V1_ID = "fr.health.test.samu1-v1";
    protected static final String SAMU2_V1_ID = "fr.health.test.samu2-v1";
    protected static final String SAMU_V3_ID = "fr.health.test.samu-v3";
    protected static final String SAMU_V3_DIRECT_CISU_ID = "fr.health.test.samu-v3-direct-cisu";
    protected static final String SDIS_Z_ID = "fr.fire.nexsis.sdisZ";
    protected static final String VHOST_15_15_V1_TAG = "15-15_v1.5";
    protected static final String VHOST_15_15_V3_TAG = "15-15_v2.1";
    protected static final String VHOST_15_NEXSIS_V3_TAG = "15-nexsis_v1.9";

    @Test
    void messageFromV1ToV1() throws Exception {
        String useCase = Files.readString(Path.of(dotenv.get("EDXL_EXAMPLE_FILE_PATH_V1")));

        String distributionId = SAMU1_V1_ID + "_" + UUID.randomUUID();

        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, SAMU2_V1_ID,
                DistributionKind.REPORT, DistributionStatus.ACTUAL);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertEquals(matched.getVhost(), "15-15_v1.5");
        assertEquals(matched.getQueue(), SAMU2_V1_ID + ".message");
    }

    @Test
    void messageFromV3ToV1() throws Exception {
        String useCase = Files.readString(Path.of(dotenv.get("EDXL_EXAMPLE_FILE_PATH_V3")));

        String distributionId = SAMU_V3_ID + "_" + UUID.randomUUID();

        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU_V3_ID, SAMU1_V1_ID,
                DistributionKind.REPORT, DistributionStatus.ACTUAL);

        sendMessage(VHOST_15_15_V3_TAG, SAMU_V3_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertEquals(matched.getVhost(), VHOST_15_15_V1_TAG);
        assertEquals(matched.getQueue(), SAMU1_V1_ID + ".message");
    }

    @Test
    void messageFromV1ToV3() throws Exception {
        String useCase = Files.readString(Path.of(dotenv.get("EDXL_EXAMPLE_FILE_PATH_V1")));

        String distributionId = SAMU1_V1_ID + "_" + UUID.randomUUID();

        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, SAMU_V3_ID,
                DistributionKind.REPORT, DistributionStatus.ACTUAL);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertEquals(matched.getVhost(), VHOST_15_15_V3_TAG);
        assertEquals(matched.getQueue(), SAMU_V3_ID + ".message");
    }

    @Test
    void messageFromV1ToNexsis() throws Exception {
        String useCase = Files.readString(Path.of(dotenv.get("EDXL_EXAMPLE_FILE_PATH_V1")));

        String distributionId = SAMU1_V1_ID + "_" + UUID.randomUUID();

        String edxlJson = new MessageBuilder().buildMessage(
                useCase, distributionId, SAMU1_V1_ID, SDIS_Z_ID,
                DistributionKind.REPORT, DistributionStatus.ACTUAL);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson);

        MessageDTO matched = awaitMessage(distributionId);

        assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertEquals(matched.getVhost(), VHOST_15_NEXSIS_V3_TAG);
        assertEquals(matched.getQueue(), SDIS_Z_ID + ".message");
    }

    @Test
    void messagePublishedBy2ProducersCanBeReceivedInMisorder() throws Exception {
        String useCase = Files.readString(Path.of(dotenv.get("EDXL_EXAMPLE_FILE_PATH_V1")));

        String distributionId1 = SAMU1_V1_ID + "_" + UUID.randomUUID();
        String distributionId2 = SAMU1_V1_ID + "_" + UUID.randomUUID();

        String edxlJson1 = new MessageBuilder().buildMessage(
                useCase, distributionId1, SAMU1_V1_ID, SAMU2_V1_ID,
                DistributionKind.REPORT, DistributionStatus.ACTUAL
        );

        String edxlJson2 = new MessageBuilder().buildMessage(
                useCase, distributionId2, SAMU1_V1_ID, SAMU2_V1_ID,
                DistributionKind.REPORT, DistributionStatus.ACTUAL);

        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson1);
        sendMessage(VHOST_15_15_V1_TAG, SAMU1_V1_ID, edxlJson2);

        MessageDTO matched2 = awaitMessage(distributionId2);
        MessageDTO matched1 = awaitMessage(distributionId1);

        assertNotNull(matched2, "Message " + distributionId2 + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertNotNull(matched1, "Message " + distributionId1 + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
    }

}
