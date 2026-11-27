package loadtesting.clients;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientPairPickerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(ClientPairPicker.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void pairsAreAllCombinationsWithDistinctClients() {
        ClientPairPicker picker = picker(pool("a", "b", "c"), pool("a", "b"));

        assertThat(picker.pairs()).containsExactlyInAnyOrder(
                new ClientPair("a", "b"),
                new ClientPair("b", "a"),
                new ClientPair("c", "a"),
                new ClientPair("c", "b"));
    }

    @Test
    void emptySenderPoolFails() {
        assertThatThrownBy(() -> picker(new ClientPool("getSenders constraint", List.of()), pool("a")))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("No sender/recipient pair available for scenario X")
                .hasMessageContaining("getSenders constraint");
    }

    @Test
    void emptyRecipientPoolFails() {
        assertThatThrownBy(() -> picker(pool("a"), new ClientPool("getRecipients constraint", List.of())))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("No sender/recipient pair available for scenario X")
                .hasMessageContaining("getRecipients constraint");
    }

    @Test
    void singleClientSharedBySendersAndRecipientsFails() {
        assertThatThrownBy(() -> picker(pool("a"), pool("a")))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("No sender/recipient pair available for scenario X");
    }

    @Test
    void singlePairOfTwoClientsWarns() {
        picker(pool("a", "b"), pool("a", "b"));

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("scenario X");
        });
    }

    @Test
    void singleClientFacingFixedClientWarnsAndAlwaysPicksIt() {
        ClientPairPicker picker = picker(pool("samu"), ClientPool.of("fire"));

        assertThat(appender.list).singleElement().extracting(ILoggingEvent::getLevel).isEqualTo(Level.WARN);
        IntStream.range(0, 20).forEach(i -> assertThat(picker.pick()).isEqualTo(new ClientPair("samu", "fire")));
    }

    @Test
    void severalPairsDoNotWarn() {
        picker(pool("a", "b"), ClientPool.of("fire"));

        assertThat(appender.list).isEmpty();
    }

    private static ClientPairPicker picker(ClientPool senders, ClientPool recipients) {
        return new ClientPairPicker("scenario X", senders, recipients);
    }

    private static ClientPool pool(String... clientIds) {
        return new ClientPool("constraint", List.of(clientIds));
    }
}
