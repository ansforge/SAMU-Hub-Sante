package tnr;

import static org.junit.jupiter.api.Assertions.*;
import static tnr.TestConstants.RECEIVE_TIMEOUT_SECS;

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
}
