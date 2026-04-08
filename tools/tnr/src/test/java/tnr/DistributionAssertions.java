package tnr;

import static org.junit.jupiter.api.Assertions.*;
import static tnr.TestConstants.*;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import tnr.dto.MessageDTO;

public final class DistributionAssertions {

    private DistributionAssertions() {
    }

    public static void assertVhostEquals(MessageDTO matched, String expectedVhost) {
        assertEquals(expectedVhost, matched.getVhost(),
                () -> String.format(
                        "Expected vhost to be '%s' but was '%s' for message '%s'",
                        expectedVhost,
                        matched.getVhost(),
                        matched.getDistributionId()
                )
        );
    }

    public static void assertQueueEquals(MessageDTO matched, String expectedQueue) {
        assertEquals(expectedQueue, matched.getQueue(),
                () -> String.format(
                        "Expected queue to be '%s' but was '%s'",
                        expectedQueue,
                        matched.getQueue()
                )
        );
    }

    public static void assertRsRi(MessageDTO rsRi, String distributionId) {
        assertNotNull(rsRi, "RS-RI " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
        assertVhostEquals(rsRi, VHOST_15_15_V3_TAG);
        assertQueueEquals(rsRi, SAMU1_V3_ID + ".message");
        assertTrue(Utils.isMessageOfType(rsRi, MessageType.RESOURCES_INFO),
                "Expected message type '%s'".formatted(MessageType.RESOURCES_INFO));
    }

    public static void assertRsSr(List<MessageDTO> messages, String expectedCaseId, Set<String> expectedResourceIds) {
        Set<String> received = new HashSet<>();
        for (MessageDTO rsSr : messages) {
            assertNotNull(rsSr, "RS-SR not received within " + RECEIVE_TIMEOUT_SECS + "s");
            assertVhostEquals(rsSr, VHOST_15_15_V3_TAG);
            assertQueueEquals(rsSr, SAMU1_V3_ID + ".message");
            assertTrue(Utils.isMessageOfType(rsSr, MessageType.RESOURCES_STATUS),
                    "Expected message type '%s'".formatted(MessageType.RESOURCES_STATUS));
            JsonNode rsNode = rsSr.getPayload()
                    .path("content").path(0)
                    .path("jsonContent").path("embeddedJsonContent").path("message")
                    .path(MessageType.RESOURCES_STATUS.value());
            assertEquals(expectedCaseId, rsNode.path("caseId").asText(), "RS-SR caseId mismatch");
            assertFalse(rsNode.has("position"), "RS-SR should not contain a 'position' field (transcoding suppression)");
            received.add(rsNode.path("resourceId").asText());
        }
        assertEquals(expectedResourceIds, received, "RS-SR resource IDs mismatch");
    }
}
